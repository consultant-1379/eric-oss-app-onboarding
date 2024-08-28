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
package com.ericsson.oss.ae.apponboarding.common.controller;

import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V1_DELETE_APP_BY_ID;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V2_ONBOARD_APP_PACKAGE;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V2_DELETE_ONBOARDING_JOB_BY_ID;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_PACKAGES_V2_API;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_LOCATION;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_NAME;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.ONBOARDING_JOB_ID;
import static com.ericsson.oss.ae.apponboarding.v2.utils.ResourceLoaderUtils.getClasspathResourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.AppPackageResponse;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.common.audit.AuditLogger;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.service.OnboardingJobsService;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.MinioTestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CoreApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@ContextConfiguration(classes = MinioTestClient.class)
public class AuditLoggingAdviceTest {

    private MockMvc mvc;
    private MockRestServiceServer mockServer;
    private Logger auditLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private ServerHttpRequest serverHttpRequest;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UrlGenerator urlGenerator;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier(APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME)
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private OnboardingJobRepository onboardingJobRepository;

    @Autowired
    private OnboardingJobsService onboardingJobService;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        auditLogger = (Logger) LoggerFactory.getLogger(AuditLogger.class);
        listAppender = new ListAppender<>();
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        auditLogger.addAppender(listAppender);
        listAppender.start();
    }

    @AfterEach
    public void resetMockServer() {
        cleanDB();
        mockServer.reset();
        listAppender.stop();
        listAppender.list.clear();
    }

    @Test
    public void verifyAuditLogSubjectIsPopulatedWhenAuthIsSet() throws Throwable {

        mvc.perform(MockMvcRequestBuilders
                .delete(String.format("/v2/onboarding-jobs/%s", ONBOARDING_JOB_ID))
                .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3MHhHV1hGRFZrcTF5bHFxYm1rMV83ZVh1b1BmTnlDNTdRZlM3MnJFSVJRIn0.eyJleHAiOjE3MDU5NTg0ODEsImlhdCI6MTcwNTk1ODE4MSwianRpIjoiODU5YzU0ZTMtY2IxNS00NGZmLTllZTgtZWUyZTYzZWI1YWE2IiwiaXNzIjoiaHR0cHM6Ly9laWMuaGFsbDEzNC14MS5ld3MuZ2ljLmVyaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6WyJtYXN0ZXItcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjVmNWVkYzI0LWFjZGUtNDVmYy1iYjUwLWJhMDU5NTYxNGYzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVwcm9yb21BZG1pbiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwiQXBwTWdyQWRtaW4iLCJVc2VyQWRtaW4iLCJBcHBNZ3JPcGVyYXRvciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwgcm9sZXMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxMC4xNTYuNzYuMjgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtZXByb3JvbWFkbWluIiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni43Ni4yOCIsImNsaWVudF9pZCI6ImVwcm9yb21BZG1pbiJ9.N5L8XqC2gjQbWqpHwGRJF7CbDt66FfFXI1mXJUhlCoslRBtiFlLS4B02GEkDTUKLcMp80WBI50LF2B006kjU0PmgZ9JlJYlS_M24LYH1wZGYPCtb91zqfRmKdrk0d-VZwl4OHKzae9os2Ebr9QkKH7iavkEAHo-A1GRxUUqbiM1oHRs_EVgGycYPRrII-6Viz32Kalhp-ZNhDAPMmqAxrb78JdtTpIvOsNnToguGonNGPjlRHZ8e3gLnqRNPPbHU_3vF-MUSo2hpPRtNKeXm5N10Al9qMf9cirFzKcqLZv6j1RQdEBHeoQ0dUvmRxdeMJRo-PFi-k9KAKIYwIoj84w")
                .header("Accept", "*/*"))
            .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();

        assertEquals("404", mdcPropertyMap.get("resp_code"));
        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("service-account-eproromadmin", mdcPropertyMap.get("subject"));
        assertEquals("129", mdcPropertyMap.get("resp_message"));
    }

    @Test
    public void verifyAuditLogSubjectIsPopulatedWhenAuthIsSetForV1() throws Throwable {

        mvc.perform(MockMvcRequestBuilders
                        .delete(String.format("/v1/apps/%s",1))
                        .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3MHhHV1hGRFZrcTF5bHFxYm1rMV83ZVh1b1BmTnlDNTdRZlM3MnJFSVJRIn0.eyJleHAiOjE3MDU5NTg0ODEsImlhdCI6MTcwNTk1ODE4MSwianRpIjoiODU5YzU0ZTMtY2IxNS00NGZmLTllZTgtZWUyZTYzZWI1YWE2IiwiaXNzIjoiaHR0cHM6Ly9laWMuaGFsbDEzNC14MS5ld3MuZ2ljLmVyaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6WyJtYXN0ZXItcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjVmNWVkYzI0LWFjZGUtNDVmYy1iYjUwLWJhMDU5NTYxNGYzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVwcm9yb21BZG1pbiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwiQXBwTWdyQWRtaW4iLCJVc2VyQWRtaW4iLCJBcHBNZ3JPcGVyYXRvciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwgcm9sZXMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxMC4xNTYuNzYuMjgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtZXByb3JvbWFkbWluIiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni43Ni4yOCIsImNsaWVudF9pZCI6ImVwcm9yb21BZG1pbiJ9.N5L8XqC2gjQbWqpHwGRJF7CbDt66FfFXI1mXJUhlCoslRBtiFlLS4B02GEkDTUKLcMp80WBI50LF2B006kjU0PmgZ9JlJYlS_M24LYH1wZGYPCtb91zqfRmKdrk0d-VZwl4OHKzae9os2Ebr9QkKH7iavkEAHo-A1GRxUUqbiM1oHRs_EVgGycYPRrII-6Viz32Kalhp-ZNhDAPMmqAxrb78JdtTpIvOsNnToguGonNGPjlRHZ8e3gLnqRNPPbHU_3vF-MUSo2hpPRtNKeXm5N10Al9qMf9cirFzKcqLZv6j1RQdEBHeoQ0dUvmRxdeMJRo-PFi-k9KAKIYwIoj84w")
                        .header("Accept", "*/*"))
                .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();

        assertEquals("404", mdcPropertyMap.get("resp_code"));
        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("service-account-eproromadmin", mdcPropertyMap.get("subject"));
        assertEquals("143", mdcPropertyMap.get("resp_message"));
    }

    @Test
    public void verifyAuditLogMessageContainsOperationV1() throws Throwable {

        mvc.perform(MockMvcRequestBuilders
                        .delete(String.format("/v1/apps/%s",123))
                        .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3MHhHV1hGRFZrcTF5bHFxYm1rMV83ZVh1b1BmTnlDNTdRZlM3MnJFSVJRIn0.eyJleHAiOjE3MDU5NTg0ODEsImlhdCI6MTcwNTk1ODE4MSwianRpIjoiODU5YzU0ZTMtY2IxNS00NGZmLTllZTgtZWUyZTYzZWI1YWE2IiwiaXNzIjoiaHR0cHM6Ly9laWMuaGFsbDEzNC14MS5ld3MuZ2ljLmVyaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6WyJtYXN0ZXItcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjVmNWVkYzI0LWFjZGUtNDVmYy1iYjUwLWJhMDU5NTYxNGYzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVwcm9yb21BZG1pbiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwiQXBwTWdyQWRtaW4iLCJVc2VyQWRtaW4iLCJBcHBNZ3JPcGVyYXRvciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwgcm9sZXMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxMC4xNTYuNzYuMjgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtZXByb3JvbWFkbWluIiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni43Ni4yOCIsImNsaWVudF9pZCI6ImVwcm9yb21BZG1pbiJ9.N5L8XqC2gjQbWqpHwGRJF7CbDt66FfFXI1mXJUhlCoslRBtiFlLS4B02GEkDTUKLcMp80WBI50LF2B006kjU0PmgZ9JlJYlS_M24LYH1wZGYPCtb91zqfRmKdrk0d-VZwl4OHKzae9os2Ebr9QkKH7iavkEAHo-A1GRxUUqbiM1oHRs_EVgGycYPRrII-6Viz32Kalhp-ZNhDAPMmqAxrb78JdtTpIvOsNnToguGonNGPjlRHZ8e3gLnqRNPPbHU_3vF-MUSo2hpPRtNKeXm5N10Al9qMf9cirFzKcqLZv6j1RQdEBHeoQ0dUvmRxdeMJRo-PFi-k9KAKIYwIoj84w")
                        .header("Accept", "*/*"))
                .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final String message = loggedEvent.getFormattedMessage();

        assertEquals(String.format(V1_DELETE_APP_BY_ID, 123)
                + " DELETE http://localhost/v1/apps/" + 123, message);
    }

    @Test
    public void verifyAuditLogSubjectIsMissingWhenHeaderIsNotSet() throws Throwable {
        mvc.perform(MockMvcRequestBuilders
                        .delete(String.format("/v2/onboarding-jobs/%s", ONBOARDING_JOB_ID))
                .header("Accept", "*/*"))
            .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();

        assertEquals("404", mdcPropertyMap.get("resp_code"));
        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("n/av", mdcPropertyMap.get("subject"));
        assertEquals("129", mdcPropertyMap.get("resp_message"));
    }

    @Test
    public void verifyAuditLogSubjectIsMissingWhenAuthIsInvalid() throws Throwable {

        mvc.perform(MockMvcRequestBuilders
                        .delete(String.format("/v2/onboarding-jobs/%s", ONBOARDING_JOB_ID))
                        .header("authorization", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3MHhHV1hGRFZrcTF5bHFxYm1rMV83ZVh1b1BmTnlDNTdRZlM3MnJFSVJRIn0.eyJleHAiOjE3MDU5NTg0ODEsImlhdCI6MTcwNTk1ODE4MSwianRpIjoiODU5YzU0ZTMtY2IxNS00NGZmLTllZTgtZWUyZTYzZWI1YWE2IiwiaXNzIjoiaHR0cHM6Ly9laWMuaGFsbDEzNC14MS5ld3MuZ2ljLmVyaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6WyJtYXN0ZXItcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjVmNWVkYzI0LWFjZGUtNDVmYy1iYjUwLWJhMDU5NTYxNGYzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVwcm9yb21BZG1pbiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwiQXBwTWdyQWRtaW4iLCJVc2VyQWRtaW4iLCJBcHBNZ3JPcGVyYXRvciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwgcm9sZXMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxMC4xNTYuNzYuMjgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtZXByb3JvbWFkbWluIiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni43Ni4yOCIsImNsaWVudF9pZCI6ImVwcm9yb21BZG1pbiJ9.N5L8XqC2gjQbWqpHwGRJF7CbDt66FfFXI1mXJUhlCoslRBtiFlLS4B02GEkDTUKLcMp80WBI50LF2B006kjU0PmgZ9JlJYlS_M24LYH1wZGYPCtb91zqfRmKdrk0d-VZwl4OHKzae9os2Ebr9QkKH7iavkEAHo-A1GRxUUqbiM1oHRs_EVgGycYPRrII-6Viz32Kalhp-ZNhDAPMmqAxrb78JdtTpIvOsNnToguGonNGPjlRHZ8e3gLnqRNPPbHU_3vF-MUSo2hpPRtNKeXm5N10Al9qMf9cirFzKcqLZv6j1RQdEBHeoQ0dUvmRxdeMJRo-PFi-k9KAKIYwIoj84w")
                .header("Accept", "*/*"))
                .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();

        assertEquals("404", mdcPropertyMap.get("resp_code"));
        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("n/av", mdcPropertyMap.get("subject"));
        assertEquals("129", mdcPropertyMap.get("resp_message"));
    }

    @Test
    public void verifyAuditLogSubjectWhenUpnIsSet() throws Throwable {
        mvc.perform(MockMvcRequestBuilders
                        .delete(String.format("/v2/onboarding-jobs/%s", ONBOARDING_JOB_ID))
                .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDRmVweVdhVjBGQjExYUhsalY3TDhyQ2l1RUlZZG9WT0JWZUxxM3lHdjNBIn0.eyJleHAiOjE3MTI3NjM0MTEsImlhdCI6MTcxMjc2MzExMSwiYXV0aF90aW1lIjoxNzEyNzYwNjMyLCJqdGkiOiIzODEzMDBlYy1hM2Q4LTQ4MzEtODExNC1kNzFhY2NmMzMxOWQiLCJpc3MiOiJodHRwczovL2VpYy5oYXJ0MDk4LXgyLmV3cy5naWMuZXJpY3Nzb24uc2UvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjpbImFkcC1pYW0tYWEtY2xpZW50IiwibWFzdGVyLXJlYWxtIiwiYWNjb3VudCJdLCJzdWIiOiJlNTIxYzk1ZS03MWM4LTQxMjEtOTdjYi01YWRiOGYwN2RmODIiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhZHAtaWFtLWFhLWNsaWVudCIsInNlc3Npb25fc3RhdGUiOiIyOWIyOWE4My1iYWIxLTRmODQtOWZmNC0xZTBmZDNiYzcxNDkiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1tYXN0ZXIiLCJVc2VyQWRtaW4iLCJMb2dWaWV3ZXJfU3lzdGVtX0FwcGxpY2F0aW9uX09wZXJhdG9yIiwiYWRtaW4iLCJMb2dBUElfRXh0QXBwc19BcHBsaWNhdGlvbl9SZWFkT25seSIsImNyZWF0ZS1yZWFsbSIsIkFwcE1nckFkbWluIiwiTG9nVmlld2VyX0V4dEFwcHNfQXBwbGljYXRpb25fT3BlcmF0b3IiLCJvZmZsaW5lX2FjY2VzcyIsIkFwcE1ncl9BcHBsaWNhdGlvbl9BZG1pbmlzdHJhdG9yIiwiTG9nVmlld2VyIiwidW1hX2F1dGhvcml6YXRpb24iLCJHQVNfVXNlciIsIlNlYXJjaEVuZ2luZVJlYWRlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Im1hc3Rlci1yZWFsbSI6eyJyb2xlcyI6WyJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsInZpZXctcmVhbG0iLCJtYW5hZ2UtaWRlbnRpdHktcHJvdmlkZXJzIiwiaW1wZXJzb25hdGlvbiIsImNyZWF0ZS1jbGllbnQiLCJtYW5hZ2UtdXNlcnMiLCJxdWVyeS1yZWFsbXMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJxdWVyeS1jbGllbnRzIiwicXVlcnktdXNlcnMiLCJtYW5hZ2UtZXZlbnRzIiwibWFuYWdlLXJlYWxtIiwidmlldy1ldmVudHMiLCJ2aWV3LXVzZXJzIiwidmlldy1jbGllbnRzIiwibWFuYWdlLWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZS1hZHAtYXV0aCByb2xlcyIsInNpZCI6IjI5YjI5YTgzLWJhYjEtNGY4NC05ZmY0LTFlMGZkM2JjNzE0OSIsInVwbiI6ImtjYWRtaW4ifQ.EudtPhUHqgYD1M1oDBok-JRBuaHxlbsgsJii3yHndS3MtYztNZkbZzM-e_1Q5El9YmqoD33CYXaGfvQ4V4sduYh6DpXO44ID93JnTzPxXbcVGWz1-C_U_2l7UtlGlA7SfYevsQnfQWXbIqRwpJz5QDJ3ufyDA2uphcTwbjRqAa444JBYiUZxWQr59BBjKQK-q7RktITBOUPBIZ33MjmR_9eXBmK_N3VT_ajgT291dws_-uV7XEvKsrQvUKwBoWbx9qx9Mrp8T7x5mvhkyrUYQe-HKU7fM7t-S7XuIn1wNzZp1LE2Pl5u-Wfd4aVlj3W3n-iKbAOxSKYmsaRNOGFP9Q")
                .header("Accept", "*/*"))
            .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();

        assertEquals("404", mdcPropertyMap.get("resp_code"));
        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("kcadmin", mdcPropertyMap.get("subject"));
        assertEquals("129", mdcPropertyMap.get("resp_message"));
    }

    @Test
    public void verifyAuditLogSubjectIsMissingWhenTokenIsInvalid() throws Throwable {
        mvc.perform(MockMvcRequestBuilders
                .delete(String.format("/v2/onboarding-jobs/%s", ONBOARDING_JOB_ID))
                .header("authorization", "Bearer IsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDRmVweVdhVjBGQjExYUhsalY3TDhyQ2l1RUlZZ")
                .header("Accept", "*/*"))
            .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();

        assertEquals("404", mdcPropertyMap.get("resp_code"));
        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("n/av", mdcPropertyMap.get("subject"));
        assertEquals("129", mdcPropertyMap.get("resp_message"));
    }

    @Test
    public void verifyAuditLogSubjectIsMissingWhenHeaderIsInvalid() throws Throwable {
        mvc.perform(MockMvcRequestBuilders
                        .delete(String.format("/v2/onboarding-jobs/%s", ONBOARDING_JOB_ID))
                        .header("name", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3MHhHV1hGRFZrcTF5bHFxYm1rMV83ZVh1b1BmTnlDNTdRZlM3MnJFSVJRIn0.eyJleHAiOjE3MDU5NTg0ODEsImlhdCI6MTcwNTk1ODE4MSwianRpIjoiODU5YzU0ZTMtY2IxNS00NGZmLTllZTgtZWUyZTYzZWI1YWE2IiwiaXNzIjoiaHR0cHM6Ly9laWMuaGFsbDEzNC14MS5ld3MuZ2ljLmVyaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6WyJtYXN0ZXItcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjVmNWVkYzI0LWFjZGUtNDVmYy1iYjUwLWJhMDU5NTYxNGYzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVwcm9yb21BZG1pbiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwiQXBwTWdyQWRtaW4iLCJVc2VyQWRtaW4iLCJBcHBNZ3JPcGVyYXRvciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwgcm9sZXMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxMC4xNTYuNzYuMjgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtZXByb3JvbWFkbWluIiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni43Ni4yOCIsImNsaWVudF9pZCI6ImVwcm9yb21BZG1pbiJ9.N5L8XqC2gjQbWqpHwGRJF7CbDt66FfFXI1mXJUhlCoslRBtiFlLS4B02GEkDTUKLcMp80WBI50LF2B006kjU0PmgZ9JlJYlS_M24LYH1wZGYPCtb91zqfRmKdrk0d-VZwl4OHKzae9os2Ebr9QkKH7iavkEAHo-A1GRxUUqbiM1oHRs_EVgGycYPRrII-6Viz32Kalhp-ZNhDAPMmqAxrb78JdtTpIvOsNnToguGonNGPjlRHZ8e3gLnqRNPPbHU_3vF-MUSo2hpPRtNKeXm5N10Al9qMf9cirFzKcqLZv6j1RQdEBHeoQ0dUvmRxdeMJRo-PFi-k9KAKIYwIoj84w")
                        .header("Accept", "*/*"))
                .andExpect(status().isNotFound());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();

        assertEquals("404", mdcPropertyMap.get("resp_code"));
        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("n/av", mdcPropertyMap.get("subject"));
        assertEquals("129", mdcPropertyMap.get("resp_message"));
    }

    @Test
    public void testOnboardAppPackage_and_job_completes_success() throws Exception {
        // Given
        // A csar to onboard
        final MockMultipartFile file = setupMultipartFile(TEST_CSAR_NAME);

        // Check if app exists in App LCM
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(Constants.TEST_CSAR_APP_NAME, Constants.TEST_CSAR_APP_VERSION);
        mockServer.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/getAppResponseEmpty.json"), MediaType.APPLICATION_JSON));

        // Create the App in App LCM
        final String urlForCreateApp = urlGenerator.generateUrlForCreateApp();
        mockServer.expect(requestTo(urlForCreateApp)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(getClasspathResourceAsString("expectedresponses/lcm/createAppResponse.json"), MediaType.APPLICATION_JSON));

        // When
        final MvcResult result = mvc.perform(multipart(APP_PACKAGES_V2_API).file(file)
            .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3MHhHV1hGRFZrcTF5bHFxYm1rMV83ZVh1b1BmTnlDNTdRZlM3MnJFSVJRIn0.eyJleHAiOjE3MDU5NTg0ODEsImlhdCI6MTcwNTk1ODE4MSwianRpIjoiODU5YzU0ZTMtY2IxNS00NGZmLTllZTgtZWUyZTYzZWI1YWE2IiwiaXNzIjoiaHR0cHM6Ly9laWMuaGFsbDEzNC14MS5ld3MuZ2ljLmVyaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6WyJtYXN0ZXItcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjVmNWVkYzI0LWFjZGUtNDVmYy1iYjUwLWJhMDU5NTYxNGYzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVwcm9yb21BZG1pbiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwiQXBwTWdyQWRtaW4iLCJVc2VyQWRtaW4iLCJBcHBNZ3JPcGVyYXRvciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwgcm9sZXMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxMC4xNTYuNzYuMjgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtZXByb3JvbWFkbWluIiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni43Ni4yOCIsImNsaWVudF9pZCI6ImVwcm9yb21BZG1pbiJ9.N5L8XqC2gjQbWqpHwGRJF7CbDt66FfFXI1mXJUhlCoslRBtiFlLS4B02GEkDTUKLcMp80WBI50LF2B006kjU0PmgZ9JlJYlS_M24LYH1wZGYPCtb91zqfRmKdrk0d-VZwl4OHKzae9os2Ebr9QkKH7iavkEAHo-A1GRxUUqbiM1oHRs_EVgGycYPRrII-6Viz32Kalhp-ZNhDAPMmqAxrb78JdtTpIvOsNnToguGonNGPjlRHZ8e3gLnqRNPPbHU_3vF-MUSo2hpPRtNKeXm5N10Al9qMf9cirFzKcqLZv6j1RQdEBHeoQ0dUvmRxdeMJRo-PFi-k9KAKIYwIoj84w"))
            .andReturn();
        Assertions.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        final UUID onboardJobId = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AppPackageResponse.class).getOnboardingJob()
            .getId();

        // Then
        waitForAppPackageProcessorThread();
        final OnboardingJob retrievedJob = onboardingJobService.getOnboardingJobById(onboardJobId);
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, retrievedJob.getStatus());

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();
        final String message = loggedEvent.getFormattedMessage();

        assertEquals(String.format(V2_ONBOARD_APP_PACKAGE, onboardJobId, TEST_CSAR_NAME)
                + " POST http://localhost/v2/app-packages", message);

    }
    @Test
    public void verifyDeleteAppAuditLog_v2_success() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .delete(String.format("/v2/onboarding-jobs/%s", ONBOARDING_JOB_ID))
                .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDRmVweVdhVjBGQjExYUhsalY3TDhyQ2l1RUlZZG9WT0JWZUxxM3lHdjNBIn0.eyJleHAiOjE3MTI3NjM0MTEsImlhdCI6MTcxMjc2MzExMSwiYXV0aF90aW1lIjoxNzEyNzYwNjMyLCJqdGkiOiIzODEzMDBlYy1hM2Q4LTQ4MzEtODExNC1kNzFhY2NmMzMxOWQiLCJpc3MiOiJodHRwczovL2VpYy5oYXJ0MDk4LXgyLmV3cy5naWMuZXJpY3Nzb24uc2UvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjpbImFkcC1pYW0tYWEtY2xpZW50IiwibWFzdGVyLXJlYWxtIiwiYWNjb3VudCJdLCJzdWIiOiJlNTIxYzk1ZS03MWM4LTQxMjEtOTdjYi01YWRiOGYwN2RmODIiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhZHAtaWFtLWFhLWNsaWVudCIsInNlc3Npb25fc3RhdGUiOiIyOWIyOWE4My1iYWIxLTRmODQtOWZmNC0xZTBmZDNiYzcxNDkiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1tYXN0ZXIiLCJVc2VyQWRtaW4iLCJMb2dWaWV3ZXJfU3lzdGVtX0FwcGxpY2F0aW9uX09wZXJhdG9yIiwiYWRtaW4iLCJMb2dBUElfRXh0QXBwc19BcHBsaWNhdGlvbl9SZWFkT25seSIsImNyZWF0ZS1yZWFsbSIsIkFwcE1nckFkbWluIiwiTG9nVmlld2VyX0V4dEFwcHNfQXBwbGljYXRpb25fT3BlcmF0b3IiLCJvZmZsaW5lX2FjY2VzcyIsIkFwcE1ncl9BcHBsaWNhdGlvbl9BZG1pbmlzdHJhdG9yIiwiTG9nVmlld2VyIiwidW1hX2F1dGhvcml6YXRpb24iLCJHQVNfVXNlciIsIlNlYXJjaEVuZ2luZVJlYWRlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Im1hc3Rlci1yZWFsbSI6eyJyb2xlcyI6WyJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsInZpZXctcmVhbG0iLCJtYW5hZ2UtaWRlbnRpdHktcHJvdmlkZXJzIiwiaW1wZXJzb25hdGlvbiIsImNyZWF0ZS1jbGllbnQiLCJtYW5hZ2UtdXNlcnMiLCJxdWVyeS1yZWFsbXMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJxdWVyeS1jbGllbnRzIiwicXVlcnktdXNlcnMiLCJtYW5hZ2UtZXZlbnRzIiwibWFuYWdlLXJlYWxtIiwidmlldy1ldmVudHMiLCJ2aWV3LXVzZXJzIiwidmlldy1jbGllbnRzIiwibWFuYWdlLWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZS1hZHAtYXV0aCByb2xlcyIsInNpZCI6IjI5YjI5YTgzLWJhYjEtNGY4NC05ZmY0LTFlMGZkM2JjNzE0OSIsInVwbiI6ImtjYWRtaW4ifQ.EudtPhUHqgYD1M1oDBok-JRBuaHxlbsgsJii3yHndS3MtYztNZkbZzM-e_1Q5El9YmqoD33CYXaGfvQ4V4sduYh6DpXO44ID93JnTzPxXbcVGWz1-C_U_2l7UtlGlA7SfYevsQnfQWXbIqRwpJz5QDJ3ufyDA2uphcTwbjRqAa444JBYiUZxWQr59BBjKQK-q7RktITBOUPBIZ33MjmR_9eXBmK_N3VT_ajgT291dws_-uV7XEvKsrQvUKwBoWbx9qx9Mrp8T7x5mvhkyrUYQe-HKU7fM7t-S7XuIn1wNzZp1LE2Pl5u-Wfd4aVlj3W3n-iKbAOxSKYmsaRNOGFP9Q")
        );

        final List<ILoggingEvent> logList = listAppender.list;
        final ILoggingEvent loggedEvent = logList.get(0);
        final Map<String, String> mdcPropertyMap = loggedEvent.getMDCPropertyMap();
        final String message = loggedEvent.getFormattedMessage();

        assertEquals("log audit", mdcPropertyMap.get("facility"));
        assertEquals("kcadmin", mdcPropertyMap.get("subject"));
        assertEquals("129", mdcPropertyMap.get("resp_message"));

        assertEquals(String.format(V2_DELETE_ONBOARDING_JOB_BY_ID, ONBOARDING_JOB_ID)
                + " DELETE http://localhost/v2/onboarding-jobs/" + ONBOARDING_JOB_ID, message);
    }

    /**
     * Wait for the asynch AppPackageProcessor thread to complete its processing
     * <p>
     * throws InterruptedException
     */
    private void waitForAppPackageProcessorThread() throws InterruptedException {
        threadPoolTaskExecutor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
    }

    @NotNull
    private static MockMultipartFile setupMultipartFile(final String testCsarName) throws IOException {
        final byte[] csarContent = FileUtils.readFileToBytes(TEST_CSAR_LOCATION);
        return new MockMultipartFile("file", testCsarName, MediaType.MULTIPART_FORM_DATA_VALUE, csarContent);
    }

    @Test
    public void verifyAuditLogIsMissingForExcludedPattern() throws Throwable {

        mvc.perform(MockMvcRequestBuilders
                .get("/actuator"));

        final List<ILoggingEvent> logList = listAppender.list;
        assertEquals(0, logList.size());
    }

    @Test
    public void verifyAuditLogIsMissingForGETRequest() throws Throwable {
        mvc.perform(MockMvcRequestBuilders
                        .get("/v2/onboarding-jobs")
                        .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3MHhHV1hGRFZrcTF5bHFxYm1rMV83ZVh1b1BmTnlDNTdRZlM3MnJFSVJRIn0.eyJleHAiOjE3MDU5NTg0ODEsImlhdCI6MTcwNTk1ODE4MSwianRpIjoiODU5YzU0ZTMtY2IxNS00NGZmLTllZTgtZWUyZTYzZWI1YWE2IiwiaXNzIjoiaHR0cHM6Ly9laWMuaGFsbDEzNC14MS5ld3MuZ2ljLmVyaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6WyJtYXN0ZXItcmVhbG0iLCJhY2NvdW50Il0sInN1YiI6IjVmNWVkYzI0LWFjZGUtNDVmYy1iYjUwLWJhMDU5NTYxNGYzNiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVwcm9yb21BZG1pbiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwiQXBwTWdyQWRtaW4iLCJVc2VyQWRtaW4iLCJBcHBNZ3JPcGVyYXRvciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwgcm9sZXMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxMC4xNTYuNzYuMjgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtZXByb3JvbWFkbWluIiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni43Ni4yOCIsImNsaWVudF9pZCI6ImVwcm9yb21BZG1pbiJ9.N5L8XqC2gjQbWqpHwGRJF7CbDt66FfFXI1mXJUhlCoslRBtiFlLS4B02GEkDTUKLcMp80WBI50LF2B006kjU0PmgZ9JlJYlS_M24LYH1wZGYPCtb91zqfRmKdrk0d-VZwl4OHKzae9os2Ebr9QkKH7iavkEAHo-A1GRxUUqbiM1oHRs_EVgGycYPRrII-6Viz32Kalhp-ZNhDAPMmqAxrb78JdtTpIvOsNnToguGonNGPjlRHZ8e3gLnqRNPPbHU_3vF-MUSo2hpPRtNKeXm5N10Al9qMf9cirFzKcqLZv6j1RQdEBHeoQ0dUvmRxdeMJRo-PFi-k9KAKIYwIoj84w")
                        .header("Accept", "*/*"))
                .andExpect(status().isOk());

        final List<ILoggingEvent> logList = listAppender.list;
        assertEquals(0, logList.size());
    }

    /**
     * Clear the DB after each test
     */
    private void cleanDB() {
        onboardingJobRepository.deleteAll();
    }
}
