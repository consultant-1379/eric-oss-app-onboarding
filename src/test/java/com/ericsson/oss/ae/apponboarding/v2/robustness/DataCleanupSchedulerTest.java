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

import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_VERSION_2;
import static com.ericsson.oss.ae.apponboarding.v2.utils.ResourceLoaderUtils.getClasspathResourceAsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.minio.MinioClient;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.service.AppPackageProcessor;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, AppPackageProcessor.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test-with-sync-processing")
@EnableRetry(proxyTargetClass = true)
public class DataCleanupSchedulerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private OnboardingJobRepository onboardingJobRepository;
    @Autowired
    private UrlGenerator urlGenerator;

    @MockBean
    private MinioClient minioClient;

    @Autowired
    private RestTemplate restTemplate;

    private static String parameterizedId;

    private MockRestServiceServer mockServer;
    private MockMvc mvc;

    @BeforeAll
    public void setup() throws IOException, InterruptedException {
        setupDB();
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION_2);
        mockServer.expect(ExpectedCount.once(), requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseDetailsParameterizedId.json",
                "{{ID_PLACEHOLDER}}",parameterizedId), MediaType.APPLICATION_JSON));

        final String url1 = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, "10.0.0");
        mockServer.expect(ExpectedCount.once(), requestTo(url1)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseEmpty.json"), MediaType.APPLICATION_JSON));

        CountDownLatch eventReceived = new CountDownLatch(4);
        eventReceived.await(40, TimeUnit.SECONDS);
    }

    @AfterAll
    public void teardown()
    {
        onboardingJobRepository.deleteAll();
    }




    @Test
    public void DataCleanupSchedulerTest_young_jobs_still_hanging() {

        List<OnboardingJobEntity> cleanedupJobs = onboardingJobRepository.findAll();
        List<OnboardingJobStatus> targetStatuses = Arrays.asList(
            OnboardingJobStatus.UPLOADED,
            OnboardingJobStatus.UNPACKED,
            OnboardingJobStatus.PARSED);

        List<OnboardingJobEntity> filteredJobs = cleanedupJobs.stream()
            .filter(job -> targetStatuses.contains(job.getStatus()))
            .collect(Collectors.toList());

        Assert.assertTrue(filteredJobs.size() == 4);

    }

    @Test
    public void DataCleanupSchedulerTest_parsed_job_successfully_onboarded() {

        List<OnboardingJobEntity> cleanedupJobs = onboardingJobRepository.findAll();
        List<OnboardingJobStatus> targetStatuses = Arrays.asList(
            OnboardingJobStatus.ONBOARDED);

        List<OnboardingJobEntity> filteredJobs = cleanedupJobs.stream()
            .filter(job -> targetStatuses.contains(job.getStatus()))
            .collect(Collectors.toList());

        Assert.assertTrue(filteredJobs.size() == 1);
    }

    @Test
    public void DataCleanupSchedulerTest_parsed_job_rollback_failed_successfully() throws InterruptedException, IOException {

        List<OnboardingJobEntity> cleanedupJobs = onboardingJobRepository.findAll();
        List<OnboardingJobStatus> targetStatuses = Arrays.asList(
            OnboardingJobStatus.ROLLBACK_FAILED);

        List<OnboardingJobEntity> filteredJobs = cleanedupJobs.stream()
            .filter(job -> targetStatuses.contains(job.getStatus()))
            .collect(Collectors.toList());

        Assert.assertTrue(filteredJobs.size() == 1);
    }

    @Test
    public void DataCleanupSchedulerTest_parsed_job_failed_successfully() throws InterruptedException, IOException {
        List<OnboardingJobEntity> cleanedupJobs = onboardingJobRepository.findAll();
        List<OnboardingJobStatus> targetStatuses = Arrays.asList(
            OnboardingJobStatus.FAILED);

        List<OnboardingJobEntity> filteredJobs = cleanedupJobs.stream()
            .filter(job -> targetStatuses.contains(job.getStatus()))
            .collect(Collectors.toList());

        Assert.assertTrue(filteredJobs.size() == 5);
    }

    private void setupDB()
    {
        onboardingJobRepository.deleteAll();
        for(int i = 0; i<=10 ; i++)
        {
            OnboardingJobEntity jobEntity = new OnboardingJobEntity();
            jobEntity.setPackageVersion(APP_PACKAGE_VERSION_2);
            jobEntity.setVendor("Ericsson");
            jobEntity.setType("rApp");
            jobEntity.setProvider("Ericsson");
            jobEntity.setFileName("fake.csar");
            jobEntity.setPackageSize("1000 MiB");
            jobEntity.setOnboardingEventEntities(new HashSet<>());
            jobEntity.setAppName(Constants.TEST_CSAR_APP_NAME);
            setJobSpecificData(i, jobEntity);
            OnboardingJobEntity savedEntity = onboardingJobRepository.save(jobEntity);
            if(i==5)
            {
                parameterizedId = jobEntity.getId().toString();
            }
        }
    }

    private static void setJobSpecificData(int jobCounter, OnboardingJobEntity jobEntity) {
        switch(jobCounter)
        {
            case 0:
                jobEntity.setStatus(OnboardingJobStatus.UPLOADED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 15 * 60 * 1000)); // 15 minutes ago
                break;
            case 1:
                jobEntity.setStatus(OnboardingJobStatus.UNPACKED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 20 * 60 * 1000)); // 20 minutes ago
                break;
            case 2:
                jobEntity.setStatus(OnboardingJobStatus.PARSED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 5 * 60 * 1000)); // 5 minutes ago
                break;
            case 3:
                jobEntity.setStatus(OnboardingJobStatus.UPLOADED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 25 * 60 * 1000)); // 25 minutes ago
                break;
            case 4:
                jobEntity.setStatus(OnboardingJobStatus.UNPACKED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 2 * 60 * 1000)); // 2 minutes ago
                break;
            case 5:
                jobEntity.setStatus(OnboardingJobStatus.PARSED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 30 * 60 * 1000)); // 30 minutes ago
                break;
            case 6:
                jobEntity.setStatus(OnboardingJobStatus.UPLOADED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 1 * 60 * 1000)); // 1 minute ago
                break;
            case 7:
                jobEntity.setStatus(OnboardingJobStatus.UNPACKED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 35 * 60 * 1000)); // 35 minutes ago
                break;
            case 8:
                jobEntity.setStatus(OnboardingJobStatus.PARSED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 7 * 60 * 1000)); // 7 minutes ago
                break;
            case 9:
                jobEntity.setStatus(OnboardingJobStatus.UPLOADED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 40 * 60 * 1000)); // 40 minutes ago
                break;
            case 10:
                jobEntity.setPackageVersion("10.0.0");
                jobEntity.setStatus(OnboardingJobStatus.PARSED);
                jobEntity.setStartTimestamp(new Timestamp(System.currentTimeMillis() - 45 * 60 * 1000)); // 45 minutes ago
                break;
        }
    }
}
