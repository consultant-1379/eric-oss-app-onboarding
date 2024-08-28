/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.ae.apponboarding.v2.robustness;

import static com.ericsson.oss.ae.apponboarding.v2.utils.ResourceLoaderUtils.getClasspathResourceAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.service.AppPackageProcessor;
import com.ericsson.oss.ae.apponboarding.v2.persistence.RetriableDataAccessService;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.MinioTestClient;

import io.minio.MinioClient;


@ContextConfiguration(classes = { MinioTestClient.class, RetriableDataAccessService.class})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, AppPackageProcessor.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test-with-sync-processing")
@EnableRetry(proxyTargetClass = true)
public class OnboardingJobsDbAvailabilityTest {
    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected UrlGenerator urlGenerator;

    @Autowired
    protected AppPackageProcessor appPackageProcessor;

    protected MockRestServiceServer mockServer;

    @Value("${onboarding-job.tempFolderLocation}")
    protected String tempFolderLocation;

    @Autowired
    private MinioClient minioClient;

    @SpyBean
    private OnboardingJobRepository onboardingJobRepository;

    protected static final String CREATE_APP_RESPONSE_SUCCESS = "expectedresponses/lcm/createAppResponse.json";
    protected static final String GET_APP_RESPONSE_EMPTY = "expectedresponses/lcm/getAppResponseEmpty.json";

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        ReflectionTestUtils.setField(urlGenerator, "appLcmHostname", "eric-oss-app-lcm");
    }
    @AfterEach
    public void resetMockServer() {
        mockServer.reset();
    }

    @Test
    public void testProcessAppPackage_failed_when_db_not_available_at_findById() throws URISyntaxException, IOException {
        // Given
        Mockito
            .doThrow(new QueryTimeoutException("Timeout during database query."))
            .when(onboardingJobRepository).findById(any());

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, UUID.randomUUID());

        // Then
        // 1 attempt and 5 retries
        verify(onboardingJobRepository, times(6)).findById((Mockito.any(UUID.class)));
    }

    @Test
    public void testProcessAppPackage_failed_when_db_not_available_at_update_status_unpacked() throws URISyntaxException, IOException {
        // Given
        setUpDefaultPackageUpload();
        final OnboardingJobEntity onboardingJob = initJobEntity(OnboardingJobStatus.UPLOADED);
        final UUID jobId = onboardingJob.getId();

        Mockito
            .doThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"))
            .when(onboardingJobRepository).saveAndFlush(Mockito.any(OnboardingJobEntity.class));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);

        // Then
        // State was not updated to UNPACKED
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();
        Assertions.assertEquals(OnboardingJobStatus.UPLOADED, onboardingJobEntity.getStatus());
        // Total 12 attempts, 6 attempts to save state UNPACKED and 6 during rollback phase to update state
        verify(onboardingJobRepository, times(12)).saveAndFlush((Mockito.any(OnboardingJobEntity.class)));
    }

    @Test
    public void testProcessAppPackage_success_with_db_retry_successful_at_save_state_unpacked() throws URISyntaxException, IOException {
        // Given
        setUpDefaultPackageUpload();
        final OnboardingJobEntity onboardingJob = initJobEntity(OnboardingJobStatus.UPLOADED);
        final UUID jobId = onboardingJob.getId();


        onboardingJob.setStatus(OnboardingJobStatus.UNPACKED);
        // 2 attempts followed by a successful request
        Mockito
            .doThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"))
            .doThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"))
            .doReturn(onboardingJob)
            .when(onboardingJobRepository).saveAndFlush(Mockito.any(OnboardingJobEntity.class));

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString(CREATE_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);

        // Then
        // Total 3 normal flow method calls to saveAndFlush(), plus 2 additional retries when DB is unavailable during save UNPACKED
       verify(onboardingJobRepository, times(5)).saveAndFlush((Mockito.any(OnboardingJobEntity.class)));
    }

    @Test
    public void testProcessAppPackage_failure_when_db_retry_fails_at_save_state_onboarded() throws URISyntaxException, IOException {
        // Given
        setUpDefaultPackageUpload();
        // final OnboardingJobEntity onboardingJob = buildOnboardingJobEntity();
        final OnboardingJobEntity onboardingJob = initJobEntity(OnboardingJobStatus.UPLOADED);
        final UUID jobId = onboardingJob.getId();

        // Set status for call to 'saveAndFlush()' with status UNPACKED
        onboardingJob.setStatus(OnboardingJobStatus.UNPACKED);

        // Set status for call to 'saveAndFlush()' with status PARSED
        final OnboardingJobEntity onboardingJobParsed = buildNewOnboardingJobEntity(onboardingJob.getId(), OnboardingJobStatus.PARSED);

        Mockito
            .doReturn(onboardingJob, onboardingJobParsed)
            .doThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"))
            .when(onboardingJobRepository).saveAndFlush(Mockito.any(OnboardingJobEntity.class));

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString(CREATE_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);

        // Then

        // 3 normal flow method calls to saveAndFlush(), plus total 12 additional retries when DB is unavailable
        // during save ONBOARDED status and during rollback. 6 is the max retries for each failed call.
        verify(onboardingJobRepository, times(14)).saveAndFlush((Mockito.any(OnboardingJobEntity.class)));
    }

    protected void setUpDefaultPackageUpload() throws URISyntaxException, IOException {
        final Path tempFolderPath = Paths.get(tempFolderLocation);
        FileUtils.copyFileToFolder(tempFolderPath, Constants.TEST_CSAR_NAME, Constants.TEST_CSAR_LOCATION);
    }

    protected OnboardingJobEntity initJobEntity(final OnboardingJobStatus status) {
        return onboardingJobRepository
            .save(OnboardingJobEntity.builder().fileName(Constants.TEST_CSAR_NAME).status(status).build());
    }

    private OnboardingJobEntity buildNewOnboardingJobEntity(final UUID id, final OnboardingJobStatus status ){
        return OnboardingJobEntity.builder()
            .id(id)
            .fileName(Constants.TEST_CSAR_NAME)
            .status(status)
            .packageSize("100MiB")
            .vendor("Ericsson")
            .packageVersion("1.0.0")
            .build();

    }
}
