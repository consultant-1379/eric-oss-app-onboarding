/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.AppPackageProcessorTestBase;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;

/**
 * Test class to verify robustness scenario's that cannot be executed in the AppPackageProcessTest class.
 * For example, where mocking is needed instead of using the minio testcontainer which has a real object-storage-mn service.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class AppPackageProcessorRobustnessTest extends AppPackageProcessorTestBase {

    @MockBean
    private MinioClient minioClient;

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
    public void testProcessAppPackage_create_bucket_failed_AND_rollbackSucceeded() throws Exception {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        mockServer.expect(anything()).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        Mockito.when(minioClient.bucketExists(Mockito.any(BucketExistsArgs.class))).thenReturn(false).thenReturn(false);
        Mockito.doThrow(new InternalException("Error creating bucket", "HTTP error"))
            .when(minioClient)
            .makeBucket(Mockito.any(MakeBucketArgs.class));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_check_bucket_exists_failed_AND_rollbackSucceeded() throws Exception {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        mockServer.expect(anything()).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        Mockito.when(minioClient.bucketExists(Mockito.any(BucketExistsArgs.class)))
            .thenThrow(new InvalidResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(), "text/plain", "no healthy upstream", "http trace") );

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_check_bucket_exists_failed_at_rollback() throws Exception {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        mockServer.expect(anything()).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        Mockito.when(minioClient.bucketExists(Mockito.any(BucketExistsArgs.class)))
            .thenReturn(false)
            .thenThrow(new InvalidResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(), "text/plain", "no healthy upstream", "http trace") );

        // Fail to create the App in App LCM
        final String badRequestErrorMessage = "Bad request sent from onboarding";
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withBadRequest()
                .body(badRequestErrorMessage));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.ROLLBACK_FAILED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_upload_artifact_failed_AND_rollback_succeeded() throws Exception {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        mockServer.expect(anything()).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        Mockito.when(minioClient.bucketExists(Mockito.any(BucketExistsArgs.class)))
            .thenReturn(false)
            .thenReturn(true);

        Mockito.when(minioClient.uploadObject(Mockito.any(UploadObjectArgs.class)))
            .thenThrow(new InvalidResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(), "text/plain", "no healthy upstream", "http trace") );

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
    }

}
