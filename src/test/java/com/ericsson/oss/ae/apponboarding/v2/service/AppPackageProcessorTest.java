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

package com.ericsson.oss.ae.apponboarding.v2.service;

import static com.ericsson.oss.ae.apponboarding.v2.utils.ResourceLoaderUtils.getClasspathResourceAsString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;
import com.ericsson.oss.ae.apponboarding.v2.AppPackageProcessorTestBase;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.lcm.AppItems;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.MinioTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;

@ContextConfiguration(classes = MinioTestClient.class)
public class AppPackageProcessorTest extends AppPackageProcessorTestBase {

    @Autowired
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
    public void testProcessAppPackage_success() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

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
    }

    @Test
    public void testProcessAppPackage_duplicate_exception() throws URISyntaxException, IOException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);

        mockServer.expect(anything()).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_DUPLICATE_DETAILS), MediaType.APPLICATION_JSON));

        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);

        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getTitle().equals(ErrorMessages.CREATE_ONBOARDING_JOB_PROBLEM_TITLE)));
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getDetail().equals(ErrorMessages.DUPLICATE_APP_EXISTS_DETAIL_APP_LCM)));
    }

    @Test
    public void testProcessAppPackage_duplicate_with_provider_exception() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);

        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getTitle().equals(ErrorMessages.CREATE_ONBOARDING_JOB_PROBLEM_TITLE)));
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getDetail().equals(ErrorMessages.DUPLICATE_APP_EXISTS_DETAIL_APP_LCM)));
    }

    @Test
    public void testProcessAppPackage_duplicate_exception_lcm_provides_null_provider() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_WITHOUT_PROVIDER_SUCCESS), MediaType.APPLICATION_JSON));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event ->
            event.getTitle().equals(ErrorMessages.CREATE_ONBOARDING_JOB_PROBLEM_TITLE)));
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event ->
            event.getDetail().equals(ErrorMessages.DUPLICATE_APP_EXISTS_DETAIL_APP_LCM)));

    }

    @Test
    public void testProcessAppPackage_success_lcm_provides_empty_list() throws URISyntaxException, IOException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();

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
    }

    @Test
    public void testProcessAppPackage_query_lcm_throws_400_bad_request() throws URISyntaxException, IOException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();
        final ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setDetail("Wrong Parameter");
        problemDetails.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problemDetails.setStatus(400);
        mockServer.expect(anything()).andExpect(method(HttpMethod.GET)).andRespond(
                withBadRequest().body(new ObjectMapper().writeValueAsString(problemDetails)));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getTitle().equals(ErrorMessages.ERROR_ACCESSING_APP_LCM)));
    }

    @Test
    public void testProcessAppPackage_query_lcm_throws_404_not_found() throws URISyntaxException, IOException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();
        mockServer.expect(anything()).andExpect(method(HttpMethod.GET)).andRespond(
            withResourceNotFound().body(getClasspathResourceAsString(CREATE_APP_RESPONSE_NOT_FOUND_ERROR)));
        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getTitle().equals(ErrorMessages.ERROR_ACCESSING_APP_LCM)));
    }

    @Test
    public void testProcessAppPackage_query_lcm_returns_invalid_body() throws URISyntaxException, IOException, InterruptedException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();
        mockServer.expect(anything()).andExpect(method(HttpMethod.GET)).andRespond(
                withSuccess().body(getClasspathResourceAsString(LCM_APP_RESPONSE_INVALID_BODY)));
        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);


        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getTitle().equals(ErrorMessages.ERROR_ACCESSING_APP_LCM)));
    }

    @Test
    public void testProcessAppPackage_query_lcm_throws_500_server_error() throws URISyntaxException, IOException, InterruptedException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();
        mockServer.expect(anything()).andExpect(method(HttpMethod.GET)).andRespond(
            withServerError().body(Constants.INTERNAL_SERVER_ERROR));
        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);


        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertTrue(onboardingJobEntity.getOnboardingEventEntities().stream().anyMatch(event -> event.getTitle().equals(ErrorMessages.ERROR_ACCESSING_APP_LCM)));
    }

    @Test
    public void testProcessAppPackage_query_lcm_throws_503_retry_exhausted() throws URISyntaxException, IOException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        mockServer.expect(times(6), anything())
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServiceUnavailable()
                        .body(Constants.SERVICE_UNAVAILABLE));

        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_query_lcm_throws_503_retry_then_accepted() throws URISyntaxException, IOException, InterruptedException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // 2 failed retries
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(times(2),requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServiceUnavailable()
                        .body(Constants.SERVICE_UNAVAILABLE));
        // Accepted on the 3rd retry
        AppItems items = new AppItems();
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET)).andRespond(withSuccess(new ObjectMapper().writeValueAsString(items), MediaType.APPLICATION_JSON));

        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString(CREATE_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_throws_UriSyntaxException() throws URISyntaxException, IOException {
        // Given
        UUID jobId = setUpDefaultPackageUploadAndCreateJob();
        ReflectionTestUtils.setField(urlGenerator, "appLcmHostname", "eric-oss-app-lcm\\");

        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        OnboardingJobEntity onboardingJobEntity = this.onboardingJobRepository.findById(jobId).orElseThrow();

        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_create_app_lcm_return_400() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String badRequestErrorMessage = "Bad request sent from onboarding";
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withBadRequest()
                .body(badRequestErrorMessage));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());

        final OnboardingEventEntity errorEvent = getSingleErrorEventFromJob(onboardingJobEntity);
        Assertions.assertNotNull(errorEvent, "No events in response - expected 1 error event.");
        Assertions.assertFalse(errorEvent.getTitle().isEmpty());
        Assertions.assertTrue(errorEvent.getDetail().contains(badRequestErrorMessage));
        Assertions.assertFalse(errorEvent.getTimestamp().toString().isEmpty());
    }

    @Test
    public void testProcessAppPackage_create_app_lcm_return_500() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withServerError()
                .body(getClasspathResourceAsString(CREATE_APP_RESPONSE_INTERNAL_SERVER_ERROR)));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());

        final OnboardingEventEntity errorEvent = getSingleErrorEventFromJob(onboardingJobEntity);
        Assertions.assertNotNull(errorEvent, "No events in response - expected 1 error event.");
        Assertions.assertFalse(errorEvent.getTitle().isEmpty());
        Assertions.assertFalse(errorEvent.getDetail().isEmpty());
        Assertions.assertFalse(errorEvent.getTimestamp().toString().isEmpty());
    }

    @Test
    public void testProcessAppPackage_create_app_lcm_return_invalid_body() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withAccepted()
                .body(getClasspathResourceAsString(LCM_APP_RESPONSE_INVALID_BODY)));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());

        final OnboardingEventEntity errorEvent = getSingleErrorEventFromJob(onboardingJobEntity);
        Assertions.assertNotNull(errorEvent, "No events in response - expected 1 error event.");
        Assertions.assertFalse(errorEvent.getTitle().isEmpty());
        Assertions.assertFalse(errorEvent.getDetail().isEmpty());
        Assertions.assertFalse(errorEvent.getTimestamp().toString().isEmpty());
    }

    @Test
    public void testProcessAppPackage_create_app_lcm_return_500_empty_body() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());

        final OnboardingEventEntity errorEvent = getSingleErrorEventFromJob(onboardingJobEntity);
        Assertions.assertNotNull(errorEvent, "No events in response - expected 1 error event.");
        Assertions.assertFalse(errorEvent.getTitle().isEmpty());
        Assertions.assertFalse(errorEvent.getDetail().isEmpty());
        Assertions.assertFalse(errorEvent.getTimestamp().toString().isEmpty());
    }

    @Test
    public void testProcessAppPackage_create_app_lcm_connection_failure() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(times(6), requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST)).andRespond(
            withException(new SocketTimeoutException()));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());

        final OnboardingEventEntity errorEvent = getSingleErrorEventFromJob(onboardingJobEntity);
        Assertions.assertNotNull(errorEvent, "No events in response - expected 1 error event.");
        Assertions.assertFalse(errorEvent.getTitle().isEmpty());
        Assertions.assertTrue(errorEvent.getDetail().contains("I/O error on POST request"));
        Assertions.assertFalse(errorEvent.getTimestamp().toString().isEmpty());
    }

    @Test
    public void testProcessAppPackage_create_app_lcm_retries_successful() throws URISyntaxException, IOException {
        // Given
        final UUID jobId = setUpDefaultPackageUploadAndCreateJob();

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString(GET_APP_RESPONSE_EMPTY), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(times(2), requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withServiceUnavailable()
                .body(Constants.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString(CREATE_APP_RESPONSE_SUCCESS), MediaType.APPLICATION_JSON));

        // When
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();

        // Then
        Assertions.assertTrue(Files.notExists(Paths.get(tempFolderLocation).resolve(jobId.toString())));
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, onboardingJobEntity.getStatus());
    }

    @Test
    public void testProcessAppPackage_fail_noneExistingJobId() {
        // Arrange
        final Path tempFolderPath = Paths.get(tempFolderLocation);
        final UUID nonExistingJobId = UUID.randomUUID();

        // Act
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, nonExistingJobId);

        // Assert
        Assertions.assertTrue(Files.notExists(tempFolderPath.resolve(nonExistingJobId.toString())));
    }

    @Test
    public void testProcessAppPackage_fail_csarNotInTmp() {
        // Arrange
        final Path tempFolderPath = Paths.get(tempFolderLocation);
        final UUID jobId = initJobEntity();

        //Act
        appPackageProcessor.processAppPackage("not_available.csar", jobId);

        // Assert
        Assertions.assertTrue(Files.notExists(tempFolderPath.resolve(jobId.toString())));
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertNotNull(onboardingJobEntity.getEndTimestamp());

        final Set<OnboardingEventEntity> onboardingEvents = onboardingJobEntity.getOnboardingEventEntities();
        Assertions.assertTrue(onboardingEvents.size() > 0);
        Assertions.assertTrue(
                onboardingEvents.iterator().next().getDetail().contains(ErrorMessages.DECOMPRESS_CSAR_NOT_AVAILABLE_IN_TMP_ERROR.split("%s")[0]));
    }

    @Test
    public void testProcessAppPackage_fail_emptyCsar() {
        // Arrange
        final Path tempFolderPath = Paths.get(tempFolderLocation);
        final UUID jobId = UUID.randomUUID();

        //Act
        appPackageProcessor.processAppPackage("", jobId);

        // Assert
        Assertions.assertTrue(Files.notExists(tempFolderPath.resolve(jobId.toString())));
    }

    @Test
    public void testProcessAppPackage_fail_emptyJobFolder() {
        // Arrange
        final Path tempFolderPath = Paths.get(tempFolderLocation);
        final UUID jobId = Mockito.mock(UUID.class, "123456789");
        final String jobFolderName = jobId.toString();
        doReturn(null).when(jobId).toString();

        //Act
        appPackageProcessor.processAppPackage(Constants.TEST_CSAR_NAME, jobId);

        // Assert
        Assertions.assertTrue(Files.notExists(tempFolderPath.resolve(Constants.TEST_CSAR_NAME)));
        Assertions.assertTrue(Files.notExists(tempFolderPath.resolve(jobFolderName)));
    }

    @Test
    void testProcessAppPackage_fail_corruptCsar() throws URISyntaxException, IOException {
        // Arrange
        final Path tempFolderPath = Paths.get(tempFolderLocation);
        FileUtils.copyFileToFolder(tempFolderPath, Constants.INVALID_ARCHIVE_CSAR_NAME, Constants.INVALID_ARCHIVE_CSAR_LOCATION);
        final UUID jobId = initJobEntity();

        // Act
        appPackageProcessor.processAppPackage(Constants.INVALID_ARCHIVE_CSAR_NAME, jobId);

        // Assert
        Assertions.assertTrue(Files.notExists(tempFolderPath.resolve(jobId.toString())));
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository.findById(jobId).orElseThrow();
        Assertions.assertEquals(OnboardingJobStatus.FAILED, onboardingJobEntity.getStatus());
        Assertions.assertNotNull(onboardingJobEntity.getEndTimestamp());

        final Set<OnboardingEventEntity> onboardingEvents = onboardingJobEntity.getOnboardingEventEntities();
        Assertions.assertTrue(onboardingEvents.size() > 0);
        Assertions.assertTrue(onboardingEvents.iterator().next().getDetail().contains(ErrorMessages.DECOMPRESS_CSAR_ERROR.split("%s")[0]));
    }

}
