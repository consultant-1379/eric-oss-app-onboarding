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

package com.ericsson.oss.ae.apponboarding.v2.controller;

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_PACKAGES_V2_API;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.COLON;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.INVALID_PATH_ONBOARDING_JOBS_API;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.ONBOARDING_JOBS_V2_API;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.SORT_V2_ERR_MSG;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DUPLICATE_APP_EXISTS_DETAIL_APP_ONBOARDING;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.QUERY_MULTIPLE_PROPERTIES_NOT_ALLOWED;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_FILE_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_SIZE;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_TYPE_RAPP;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_VENDOR_ERICSSON;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_VERSION;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_APP_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_INVALID_PATH_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_LOCATION;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_WITH_PROVIDER_LOCATION;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_WITH_PROVIDER_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_MULTPLE_COMPONENT_CSAR_LOCATION;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_MULTPLE_COMPONENT_CSAR_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.ResourceLoaderUtils.getClasspathResourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.api.v2.model.AppPackageResponse;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEvent;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;
import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingEventRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.RepositoryTestUtils;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.service.OnboardingJobsService;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.MinioTestClient;
import com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CoreApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@ContextConfiguration(classes = MinioTestClient.class)
public class OnboardingJobsIntegrationTest {

    //keep the logger initialized here for future uses
    private static final Logger logger = LoggerFactory.getLogger(OnboardingJobsIntegrationTest.class);
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    @Qualifier(APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME)
    ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private OnboardingJobRepository onboardingJobRepository;

    @Autowired
    private OnboardingEventRepository onboardingEventRepository;
    @Autowired
    private OnboardingJobsService onboardingJobService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UrlGenerator urlGenerator;

    private MockRestServiceServer mockServer;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;

    @BeforeEach
    public void setUp() throws IOException {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TestUtils.createDirForCsarUpload(tempFolderLocation);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    public void cleanUp() {
        cleanDB();
        mockServer.reset();
    }

    @Test
    public void testOnboardAppPackageSingleComponent() throws Exception {
        // Given
        // A csar to onboard
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_NAME, TEST_CSAR_LOCATION);

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseEmpty.json"), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/createAppResponse.json"), MediaType.APPLICATION_JSON));

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        Assertions.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        // Then
        waitForAppPackageProcessorThread();
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardJobId);
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, retrievedJob.getStatus());
    }

    @Test
    public void testOnboardAppPackageSingleComponentWithDifferentProvider() throws Exception {
        // Given
        // A csar to onboard
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_WITH_PROVIDER_NAME, TEST_CSAR_WITH_PROVIDER_LOCATION);

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseWithDifferentProviderDetails.json"), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/createAppResponse.json"), MediaType.APPLICATION_JSON));

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        Assertions.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        // Then
        waitForAppPackageProcessorThread();
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardJobId);
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, retrievedJob.getStatus());
    }

    @Test
    public void testOnboardAppPackageSingleComponentWithProviderWhenAppExistWithoutProvider() throws Exception {
        // Given
        // A csar to onboard
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_WITH_PROVIDER_NAME, TEST_CSAR_WITH_PROVIDER_LOCATION);

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseWithoutProviderDetails.json"), MediaType.APPLICATION_JSON));

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        Assertions.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        // Then
        waitForAppPackageProcessorThread();
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardJobId);
        Assertions.assertEquals(OnboardingJobStatus.FAILED, retrievedJob.getStatus());
    }

    @Test
    public void testOnboardAppPackageMultipleComponent_and_job_completes_success() throws Exception {
        // Given
        // A csar to onboard
        final MockMultipartFile file = setupMultipartFile(TEST_MULTPLE_COMPONENT_CSAR_NAME, TEST_MULTPLE_COMPONENT_CSAR_LOCATION);

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseEmpty.json"), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/createAppResponseMultipleComponent.json"), MediaType.APPLICATION_JSON));

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        Assertions.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        // Then
        waitForAppPackageProcessorThread();
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardJobId);
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, retrievedJob.getStatus());
    }

    @Deprecated
    @Test
    public void testOnboardAppPackageSingleComponent_appNotOnboardedInV1() throws Exception {
        // Given
        // A csar to onboard
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_NAME, TEST_CSAR_LOCATION);

        OnboardV1App(ApplicationStatus.ONBOARDED, "App-Onboarding-helloWorld", "2.0.0");
        OnboardV1App(ApplicationStatus.FAILED,"App-Onboarding-helloWorld", "1.0.0");
        OnboardV1App(ApplicationStatus.UPLOADED,"App-Onboarding-helloWorld", "1.0.0");

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseWithDifferentProviderDetails.json"), MediaType.APPLICATION_JSON));

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        Assertions.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/createAppResponseMultipleComponent.json"), MediaType.APPLICATION_JSON));


        // Then
        waitForAppPackageProcessorThread();
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardJobId);
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, retrievedJob.getStatus());
    }


    @Deprecated
    @Test
    public void testOnboardAppPackageSingleComponent_appExistsInV1() throws Exception {
        // Given
        // A csar to onboard
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_NAME, TEST_CSAR_LOCATION);

        OnboardV1App(ApplicationStatus.ONBOARDED, "App-Onboarding-helloWorld", "1.0.0");
        OnboardV1App(ApplicationStatus.FAILED,"", "");
        OnboardV1App(ApplicationStatus.UPLOADED,"App-Onboarding-helloWorld", "2.0.0");

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseEmpty.json"), MediaType.APPLICATION_JSON));

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        Assertions.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        // Then
        waitForAppPackageProcessorThread();
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardJobId);
        Assertions.assertEquals(OnboardingJobStatus.FAILED, retrievedJob.getStatus());
        retrievedJob.getEvents().stream().anyMatch(event -> event.getDetail().equals(DUPLICATE_APP_EXISTS_DETAIL_APP_ONBOARDING));
    }

    @Test
    public void testOnboardAppPackage_badRequest() throws Exception {
        // Given
        final MockMultipartFile file = new MockMultipartFile("file", "badFileType.txt", MediaType.MULTIPART_FORM_DATA_VALUE,
                "Hello World!".getBytes());

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        final ProblemDetails problemDetails = getProblemDetailsFromResult(result);

        //Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), problemDetails.getTitle());
        Assertions.assertTrue(problemDetails.getDetail().contains(Consts.FILE_TYPE_INVALID.split("%s")[0]));
    }

    @Test
    public void testOnboardAppPackage_emptyFile() throws Exception {
        // Given
        final MockMultipartFile file = new MockMultipartFile("file", null, MediaType.MULTIPART_FORM_DATA_VALUE, new byte[]{});

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        final ProblemDetails problemDetails = getProblemDetailsFromResult(result);

        //Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), problemDetails.getTitle());
        Assertions.assertTrue(problemDetails.getDetail().contains(Consts.FILE_TYPE_INVALID.split("%s")[0]));
    }

    @Test
    public void testOnboardAppPackage_not_found() throws Exception {
        // Given
        final MockMultipartFile file = new MockMultipartFile("file", "badFileType.txt", MediaType.MULTIPART_FORM_DATA_VALUE,
                "Hello World!".getBytes());

        // When
        final MvcResult result = mvc.perform(multipart("/v2/" + INVALID_PATH_ONBOARDING_JOBS_API).file(file)).andReturn();

        //Then
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void testOnboardAppPackage_invalidFileName() throws Exception {
        // Given
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_INVALID_PATH_NAME + TEST_CSAR_NAME, TEST_CSAR_LOCATION);
        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        //Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
    }

    @Test
    public void testOnboardAppPackage_ObjectStoreBucketAlreadyExists() throws Exception {
        // Given
        mockServer.expect(anything()).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseEmpty.json"), MediaType.APPLICATION_JSON));
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_NAME, TEST_CSAR_LOCATION);
        UUID mockUUID = UUID.randomUUID();
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(String.valueOf(mockUUID)).build());

        try (MockedStatic<UUID> mockedStatic = Mockito.mockStatic(UUID.class)) {
            mockedStatic.when(UUID::randomUUID).thenReturn(mockUUID);
            // When
            mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
            waitForAppPackageProcessorThread();

            final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(mockUUID);

            Assertions.assertEquals(OnboardingJobStatus.PARSED, retrievedJob.getStatus());
        }
    }

    @Test
    public void testGetOnboardingJobDetails_ok() throws Exception {
        // Given
        final OnboardingJobEntity onboardingJob = onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        // When
        final MvcResult result = mvc.perform(get(String.format("%s/%s", ONBOARDING_JOBS_V2_API, onboardingJob.getId().toString()))
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        // Then
        Assertions.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains(APP_PACKAGE_FILE_NAME));
        assertTrue(result.getResponse().getContentAsString().contains(APP_PACKAGE_VENDOR_ERICSSON));
        assertTrue(result.getResponse().getContentAsString().contains(APP_PACKAGE_TYPE_RAPP));
        assertTrue(result.getResponse().getContentAsString().contains(OnboardingJobStatus.ONBOARDED.toString()));
    }

    @Test
    public void testGetOnboardingJobWithSortedEvents_ok() throws Exception {
        // Given
        final OnboardingJobEntity onboardingJob = onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJobWithoutEvents(OnboardingJobStatus.ONBOARDED));
        saveEventsInDB(onboardingJob,RepositoryTestUtils.dummyOnboardingEvents());
        // When
        final MvcResult result = mvc.perform(get(String.format("%s/%s", ONBOARDING_JOBS_V2_API, onboardingJob.getId().toString()))
            .accept(MediaType.APPLICATION_JSON)).andReturn();
        // Then
        assertTrue(result.getResponse().getContentAsString().contains(OnboardingJobStatus.ONBOARDED.toString()));
        final OnboardingJob responseJob = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .readValue(result.getResponse().getContentAsString(), OnboardingJob.class);
        List<OnboardingEvent> events = responseJob.getEvents();
        assertTrue(isSorted(events), "Events are not sorted by time as expected");
    }

    @Test
    public void testGetAllJobsMvcAPI_ok() throws Exception {

        final MvcResult result = mvc.perform(get(ONBOARDING_JOBS_V2_API).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void testGetOnboardingJobById_ok() throws InterruptedException {

        // Given
        final MockMultipartFile file = new MockMultipartFile("file", "hello world.csar", MediaType.MULTIPART_FORM_DATA_VALUE,
                "Hello World!".getBytes());
        final AppPackageResponse appPackageResponse = onboardingJobService.onboardAppPackage(file);
        waitForAppPackageProcessorThread();
        final UUID onboardedJobId = appPackageResponse.getOnboardingJob().getId();
        // When the Job is retrieved by Id
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardedJobId);
        final UUID retrievedJobId = retrievedJob.getId();
        // Then both ids are the same
        Assertions.assertEquals(onboardedJobId, retrievedJobId);
    }

    @Test
    public void testGetOnboardingJobById_not_found() throws Exception {
        // Given
        final UUID notExistingJobId = UUID.randomUUID();

        // When
        final MvcResult getJobResult = mvc.perform(get(String.format("%s/%s", ONBOARDING_JOBS_V2_API, notExistingJobId))
            .accept(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_PROBLEM_JSON)).andReturn();
        final ProblemDetails problemDetails = getProblemDetailsFromResult(getJobResult);

        // Then
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), getJobResult.getResponse().getStatus());
        Assertions.assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), problemDetails.getTitle());
        Assertions.assertTrue(problemDetails.getDetail().contains(ErrorMessages.ONBOARDING_JOB_ENTITY_NOT_FOUND.split("%s")[0]));
    }

    @Test
    public void testGetOnboardingJobById_throwException() {

        // Given
        final UUID onboardedJobId = UUID.randomUUID();
        // When
        // Then
        assertThrows(OnboardingJobException.class, () -> onboardingJobService.getOnboardingJobById(onboardedJobId));
    }

    @Test
    public void testGetAllOnboardingJobs_withFileNameQueryOnly_success() throws Exception {
        // Given
        final String testFileName = "eric-oss-test-app.csar";
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        final OnboardingJobEntity job1 = RepositoryTestUtils.dummyOnboardingJob();
        job1.setFileName(testFileName);
        onboardingJobRepository.save(job1);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "fileName=", testFileName))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        assertTrue(result.getResponse().getContentAsString().contains(testFileName));
        assertFalse(result.getResponse().getContentAsString().contains(APP_PACKAGE_FILE_NAME));
    }

    @Test
    public void testGetAllOnboardingJobs_withVendorQueryOnly_success() throws Exception {
        // Given
        final String vendor = "OtherCompany";
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        final OnboardingJobEntity job1 = RepositoryTestUtils.dummyOnboardingJob();
        job1.setVendor(vendor);
        onboardingJobRepository.save(job1);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "vendor=", vendor))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        assertTrue(result.getResponse().getContentAsString().contains(vendor));
        assertFalse(result.getResponse().getContentAsString().contains(APP_PACKAGE_VENDOR_ERICSSON));
    }

    @Test
    public void testGetAllOnboardingJobs_withPackageVersionQueryOnly_success() throws Exception {
        // Given
        final String packageVersion = "1.0.0-2";
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        final OnboardingJobEntity job1 = RepositoryTestUtils.dummyOnboardingJob();
        job1.setPackageVersion(packageVersion);
        onboardingJobRepository.save(job1);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "packageVersion=", packageVersion))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        assertTrue(result.getResponse().getContentAsString().contains(packageVersion));
        assertFalse(result.getResponse().getContentAsString().contains(APP_PACKAGE_VERSION));
    }

    @Test
    public void testGetAllOnboardingJobs_withIdQueryOnly_success() throws Exception {
        // Given
        final OnboardingJobEntity job1 = onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        OnboardingJobEntity job2 = RepositoryTestUtils.dummyOnboardingJob();
        job2.setFileName("eric-oss-test-app.csar");
        job2 = onboardingJobRepository.save(job2);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "id=", job2.getId()))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        assertTrue(result.getResponse().getContentAsString().contains(job2.getId().toString()));
        assertFalse(result.getResponse().getContentAsString().contains(job1.getId().toString()));
    }

    @Test
    public void testGetAllOnboardingJobs_withTypeQueryOnly_success() throws Exception {
        // Given
        final String type = "other";
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        final OnboardingJobEntity job1 = RepositoryTestUtils.dummyOnboardingJob();
        job1.setType(type);
        onboardingJobRepository.save(job1);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "type=", type))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        assertTrue(result.getResponse().getContentAsString().contains(type));
        assertFalse(result.getResponse().getContentAsString().contains(APP_PACKAGE_TYPE_RAPP));
    }

    @Test
    public void testGetAllOnboardingJobs_with_MultipleParams_failure() throws Exception {
        // Given
        final String type = "other";
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        final OnboardingJobEntity job1 = RepositoryTestUtils.dummyOnboardingJob();
        job1.setType(type);
        onboardingJobRepository.save(job1);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s&&fileName=%s", ONBOARDING_JOBS_V2_API, "type=", type, APP_PACKAGE_FILE_NAME))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains(QUERY_MULTIPLE_PROPERTIES_NOT_ALLOWED));
    }

    @Test
    public void testGetAllOnboardingJobs_withPackageSizeQueryOnly_success() throws Exception {

        // Given
        final String packageSize = "123.4568MiB";
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        final OnboardingJobEntity job1 = RepositoryTestUtils.dummyOnboardingJob();
        job1.setPackageSize(packageSize);
        onboardingJobRepository.save(job1);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "packageSize=", packageSize))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        assertTrue(result.getResponse().getContentAsString().contains(packageSize));
        assertFalse(result.getResponse().getContentAsString().contains(APP_PACKAGE_SIZE));
    }

    @Test
    public void testGetAllOnboardingJobs_withStatusQueryOnly_success() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        final OnboardingJobEntity job1 = RepositoryTestUtils.dummyOnboardingJob();
        job1.setStatus(OnboardingJobStatus.FAILED);
        onboardingJobRepository.save(job1);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "status=", OnboardingJobStatus.FAILED))
            .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        assertTrue(result.getResponse().getContentAsString().contains(OnboardingJobStatus.FAILED.toString()));
        assertFalse(result.getResponse().getContentAsString().contains(OnboardingJobStatus.ONBOARDED.toString()));
    }

    @Test
    public void testGetAllOnboardingJobs_withAppIdQueryOnly_success() throws Exception {
        // Given
        final OnboardingJobEntity job1 = onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        OnboardingJobEntity job2 = RepositoryTestUtils.dummyOnboardingJob();
        job2.setFileName("eric-oss-test-app.csar");
        job2 = onboardingJobRepository.save(job2);

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s", ONBOARDING_JOBS_V2_API, "appId=", job2.getAppId()))
            .accept(MediaType.APPLICATION_JSON)).andReturn();
        // Then
        assertTrue(result.getResponse().getContentAsString().contains(job2.getAppId()));
        assertFalse(result.getResponse().getContentAsString().contains(job1.getAppId()));
    }

    @Test
    public void testGetAllOnboardingJobs_withInvalidQuery_param_ignored() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s", ONBOARDING_JOBS_V2_API, "filename=test.pdf"))
            .accept(MediaType.APPLICATION_JSON)).andReturn();
        // Then
        Assertions.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    }

    @Test
    public void testGetAllOnboardingJobs_withTwoColonsInQuery_emptyResponse() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s%s%s", ONBOARDING_JOBS_V2_API, "fileName=", APP_PACKAGE_FILE_NAME, COLON))
            .accept(MediaType.APPLICATION_JSON)).andReturn();
        // Then
        Assertions.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        assertEquals("{\"items\":[]}", result.getResponse().getContentAsString());
    }

    @Test
    public void testGetAllOnboardingJobs_withInvalidQueryAndInvalidSort_failed() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob());

        // When
        final MvcResult result = mvc.perform(get(String.format("%s?%s", ONBOARDING_JOBS_V2_API, "filename=test.pdf&sort=packageversion"))
            .accept(MediaType.APPLICATION_JSON)).andReturn();
        // Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains(SORT_V2_ERR_MSG));
    }

    @Test
    public void testDeleteOnboardingJob_no_content() throws Exception {
        // Given
        final OnboardingJobEntity onboardingJob = onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.ONBOARDED));
        // When
        final MvcResult deleteResult = mvc.perform(delete(String.format("%s/%s", ONBOARDING_JOBS_V2_API, onboardingJob.getId().toString()))
                .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn();

        // Then
        Assertions.assertEquals(HttpStatus.NO_CONTENT.value(), deleteResult.getResponse().getStatus());
    }

    @Test
    public void testDeleteOnboardingJob_not_found() throws Exception {
        // Given
        final UUID notExistingJobId = UUID.randomUUID();

        // When
        final MvcResult deleteResult = mvc.perform(delete(String.format("%s/%s", ONBOARDING_JOBS_V2_API, notExistingJobId))
                .accept(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_PROBLEM_JSON)).andReturn();
        final ProblemDetails problemDetails = getProblemDetailsFromResult(deleteResult);

        // Then
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), deleteResult.getResponse().getStatus());
        Assertions.assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), problemDetails.getTitle());
        Assertions.assertTrue(problemDetails.getDetail().contains(ErrorMessages.ONBOARDING_JOB_ENTITY_NOT_FOUND.split("%s")[0]));
    }

    @Test
    public void testDeleteOnboardingJob_not_found_invalid_path() throws Exception {
        // Given

        // When
        final MvcResult deleteResult = mvc.perform(delete("/v2/onboardingg-jobss/1")
                .accept(MediaType.APPLICATION_JSON)).andReturn();

        // Then
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), deleteResult.getResponse().getStatus());
    }

    @Test
    public void testDeleteOnboardingJob_() throws Exception {
        // Given
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_NAME, TEST_CSAR_LOCATION);
        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        waitForAppPackageProcessorThread();

        final OnboardingJobEntity retrievedJob = onboardingJobRepository.findById(onboardJobId).get();
        retrievedJob.setStatus(OnboardingJobStatus.ROLLBACK_FAILED);
        final OnboardingJobEntity onboardingJob = onboardingJobRepository.saveAndFlush(retrievedJob);
        final MvcResult deleteResult = mvc.perform(delete(String.format("%s/%s", ONBOARDING_JOBS_V2_API, onboardingJob.getId().toString()))
            .accept(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn();
        //Then
        Assertions.assertEquals(HttpStatus.NO_CONTENT.value(), deleteResult.getResponse().getStatus());
    }

    /**
     * Wait for the asynch AppPackageProcessor thread to complete its processing
     * <p>
     * throws InterruptedException
     */
    private void waitForAppPackageProcessorThread() throws InterruptedException {
        threadPoolTaskExecutor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
    }

    /**
     * Clear the DB after each test
     */
    private void cleanDB() {
        onboardingJobRepository.deleteAll();
        applicationRepository.deleteAll();
    }

    private ProblemDetails getProblemDetailsFromResult(final MvcResult onboardResult) throws UnsupportedEncodingException, JsonProcessingException {
        final String bodyAsString = onboardResult.getResponse().getContentAsString();
        return new ObjectMapper().readValue(bodyAsString, ProblemDetails.class);
    }

    private void saveEventsInDB(final OnboardingJobEntity onboardingJob, final Set<OnboardingEventEntity> events) {
        events.forEach((event)-> {event.setOnboardingJobEntity(onboardingJob);onboardingEventRepository.save(event);});
    }

    private boolean isSorted(final List<OnboardingEvent> sortedEvents){
        final Comparator<OnboardingEvent> onboardingEventTimestampComparator = Comparator.comparing(OnboardingEvent::getOccurredAt);
        Iterator<OnboardingEvent> iter = sortedEvents.iterator();
        OnboardingEvent current, previous = iter.next();
        while (iter.hasNext()) {
            current = iter.next();
            if (onboardingEventTimestampComparator.compare(previous, current) > 0) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    @NotNull
    private static MockMultipartFile setupMultipartFile(final String testCsarName, final String testCsarLocation) throws IOException {
        final byte[] csarContent = FileUtils.readFileToBytes(testCsarLocation);
        return new MockMultipartFile("file", testCsarName, MediaType.MULTIPART_FORM_DATA_VALUE, csarContent);
    }

    private void OnboardV1App(ApplicationStatus applicationStatus, String name, String version) {
        Application app = new Application();
        app.setStatus(applicationStatus);
        app.setName(name);
        app.setVersion(version);
        applicationRepository.save(app);
    }
}
