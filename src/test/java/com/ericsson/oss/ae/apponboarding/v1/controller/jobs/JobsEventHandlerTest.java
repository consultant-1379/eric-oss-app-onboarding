/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

package com.ericsson.oss.ae.apponboarding.v1.controller.jobs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v1.TestKubernetesServerConfig;
import com.ericsson.oss.ae.apponboarding.v1.controller.ApplicationController;
import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.service.jobs.JobsService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import okhttp3.mockwebserver.RecordedRequest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, ApplicationController.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextConfiguration(classes = TestKubernetesServerConfig.class)
public class JobsEventHandlerTest {

    private static final String FILE_NAME = "hello.csar";
    private static final String APPS_VERSION = Consts.API_V1_APPS;
    private static final String NAMESPACE = "test";
    private static final int EVENT_CHECK_COUNT = 10;
    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mvc;
    @Autowired public KubernetesClient client;

    @Autowired public TestKubernetesServerConfig testKubernetesServerConfig;

    @Autowired public ApplicationRepository applicationRepository;

    @Autowired JobsService jobsService;

    private final Logger logger = LoggerFactory.getLogger(JobsEventHandlerTest.class);

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @Test
    public void jobCompleteTest() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_NAME, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        mvc.perform(multipart(APPS_VERSION).file(file));
        List<Job> jobList = client.batch().v1().jobs().list().getItems();
        String name = jobList.get(0).getMetadata().getName();
        client.batch().v1().jobs().inNamespace(NAMESPACE).resource(makeJob("Complete", name)).updateStatus();
        RecordedRequest req = waitForDeleteEvent();
        Assertions.assertTrue(req.getPath().contains("/apis/batch/v1/namespaces/test/jobs/" + name));
        Assertions.assertEquals("DELETE", req.getMethod());

    }

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @Test
    public void jobFailedTest() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_NAME, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        mvc.perform(multipart(APPS_VERSION).file(file));
        List<Job> jobList = client.batch().v1().jobs().list().getItems();
        String name = jobList.get(0).getMetadata().getName();
        client.batch().v1().jobs().inNamespace(NAMESPACE).resource(makeJob("Failed", name)).updateStatus();
        RecordedRequest req = waitForDeleteEvent();
        Assertions.assertTrue(req.getPath().contains("/apis/batch/v1/namespaces/test/jobs/" + name));
        Assertions.assertEquals("DELETE", req.getMethod());
    }

    public Job makeJob(String statusMessage, String name) {
        final Job job = new Job();
        final JobSpec jobSpec = new JobSpec();
        jobSpec.setBackoffLimit(2);
        job.setSpec(jobSpec);
        JobStatus status = new JobStatus();
        JobCondition jobCondition = new JobCondition();
        jobCondition.setType(statusMessage);
        status.getConditions().add(jobCondition);
        job.setStatus(status);
        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        job.setMetadata(metadata);
        return job;
    }

    private RecordedRequest waitForDeleteEvent() throws InterruptedException {
        final KubernetesServer k8Server = testKubernetesServerConfig.getTestServer();
        RecordedRequest req = null;
        final CountDownLatch latch = new CountDownLatch(1);
        int i = 0;

        while (i<EVENT_CHECK_COUNT) {
            final boolean ignore = latch.await(100, TimeUnit.MILLISECONDS);
            req = k8Server.getLastRequest();
            logger.debug("Count: {}, Path: {}, Method: {}", i, req.getPath(), req.getMethod());
            i++;

            if ("DELETE".equalsIgnoreCase( req.getMethod())) {
                latch.countDown();
                break;
            } else if (i == EVENT_CHECK_COUNT) {
                latch.countDown();
            }
        }
        return req;
    }

}