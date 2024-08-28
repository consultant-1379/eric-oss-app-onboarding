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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.CannotCreateTransactionException;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.AppPackageProcessorTestBase;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingEventRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.RepositoryTestUtils;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.RetriableDataAccessService;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.MinioTestClient;

import io.minio.MinioClient;

@ContextConfiguration(classes = { MinioTestClient.class, RetriableDataAccessService.class})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class OnboardingEventDbAvailabilityTest extends AppPackageProcessorTestBase {

    @Autowired
    private MinioClient minioClient;

    @MockBean
    private OnboardingEventRepository onboardingEventRepository;

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
    public void testProcessAppPackage_failed_when_db_not_available_at_create_event() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        when(onboardingEventRepository.save(Mockito.any()))
            .thenThrow(Mockito.mock(CannotCreateTransactionException.class));

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString(CREATE_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.PARSED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_retry_success_when_db_not_available_at_create_event() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();
        OnboardingEventEntity eventToSave = RepositoryTestUtils.dummyOnboardingEvent();

        when(onboardingEventRepository.save(Mockito.any()))
            .thenThrow(Mockito.mock(CannotCreateTransactionException.class))
            .thenThrow(Mockito.mock(CannotCreateTransactionException.class))
            .thenReturn(eventToSave);


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
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, onboardingJobEntity.getStatus());
        // 3 invocations for first artifact, then 1 attempt each for the other 2 artifacts which dont need retries.
        verify(onboardingEventRepository, times(5)).save((Mockito.any(OnboardingEventEntity.class)));
    }

}
