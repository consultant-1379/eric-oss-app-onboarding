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

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_PACKAGES_V2_API;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.ONBOARDING_JOBS_V2_API;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionException;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.RepositoryTestUtils;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.service.FileService;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class OnboardingJobsRobustnessTest {

    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private FileService fileService;

    @MockBean
    private OnboardingJobRepository onboardingJobRepository;

    @MockBean
    private MinioClient minioClient;

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;

    @BeforeEach
    public void setUp() throws IOException {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TestUtils.createDirForCsarUpload(tempFolderLocation);
    }

    @AfterEach
    public void cleanUp() {
        cleanDB();
    }

    @Test
    public void testCreateOnboardingJobs_transaction_error() throws Exception {
        // Given
        final byte[] csarContent = FileUtils.readFileToBytes(TEST_CSAR_LOCATION);
        final MockMultipartFile file = new MockMultipartFile("file", TEST_CSAR_NAME, MediaType.MULTIPART_FORM_DATA_VALUE, csarContent);
        when(onboardingJobRepository.save(Mockito.any())).thenThrow(Mockito.mock(TransactionException.class));
        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();

        //Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("Transaction error during database access."));

    }

    @Test
    public void testOnboardingJobs_DataAccessException_error() throws Exception {
        // Given
        final byte[] csarContent = FileUtils.readFileToBytes(TEST_CSAR_LOCATION);
        final MockMultipartFile file = new MockMultipartFile("file", TEST_CSAR_NAME, MediaType.MULTIPART_FORM_DATA_VALUE, csarContent);
        when(onboardingJobRepository.save(Mockito.any())).thenThrow(Mockito.mock(DataAccessException.class));
        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)).andReturn();

        //Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("Data access error during communication to database."));
    }

    @Test
    public void testDeleteOnboardingJob_bad_request_invalid_job_state() throws Exception {
        // Given
        when(onboardingJobRepository.findById(ONBOARDING_JOB_ID)).thenReturn(
            Optional.ofNullable(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED)));

        // When
        final MvcResult deleteResult = mvc.perform(delete(String.format("%s/%s", ONBOARDING_JOBS_V2_API, ONBOARDING_JOB_ID))
            .accept(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn();
        final ProblemDetails problemDetails = getProblemDetailsFromResult(deleteResult);

        // Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), deleteResult.getResponse().getStatus());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), problemDetails.getTitle());
        Assertions.assertTrue(problemDetails.getDetail().contains(ErrorMessages.DELETE_JOB_INVALID_STATE.split("%s")[0]));
    }

    @Test
    public void testDeleteOnboardingJob_with_handle_rollback_failed_ok() throws Exception {
        // Given
        when(onboardingJobRepository.findById(ONBOARDING_JOB_ID)).thenReturn(
            Optional.ofNullable(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.ROLLBACK_FAILED)));
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(ONBOARDING_JOB_ID.toString()).build();
        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(false);
        // When
        final MvcResult deleteResult = mvc.perform(delete(String.format("%s/%s", ONBOARDING_JOBS_V2_API, ONBOARDING_JOB_ID))
            .accept(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn();

        // Then
        Assertions.assertEquals(HttpStatus.NO_CONTENT.value(), deleteResult.getResponse().getStatus());
    }

    private void cleanDB() {
        onboardingJobRepository.deleteAll();
    }

    private ProblemDetails getProblemDetailsFromResult(final MvcResult onboardResult) throws UnsupportedEncodingException, JsonProcessingException {
        final String bodyAsString = onboardResult.getResponse().getContentAsString();
        return new ObjectMapper().readValue(bodyAsString, ProblemDetails.class);
    }
}
