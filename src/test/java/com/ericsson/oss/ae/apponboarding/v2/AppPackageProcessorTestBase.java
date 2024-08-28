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

package com.ericsson.oss.ae.apponboarding.v2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEventType;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.service.AppPackageProcessor;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;

/**
 * Base class for AppPackageProcessor tests. Contains common fields and methods for subclasses.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, AppPackageProcessor.class})
@ActiveProfiles("test-with-sync-processing")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableRetry(proxyTargetClass = true)
public class AppPackageProcessorTestBase {

    @Autowired
    protected OnboardingJobRepository onboardingJobRepository;

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected UrlGenerator urlGenerator;

    @Autowired
    protected AppPackageProcessor appPackageProcessor;

    protected MockRestServiceServer mockServer;

    @Value("${onboarding-job.tempFolderLocation}")
    protected String tempFolderLocation;

    protected static final String GET_APP_RESPONSE_SUCCESS = "expectedresponses/lcm/getAppResponseDetails.json";
    protected static final String GET_APP_RESPONSE_WITHOUT_PROVIDER_SUCCESS = "expectedresponses/lcm/getAppResponseWithoutProviderDetails.json";
    protected static final String GET_APP_RESPONSE_EMPTY = "expectedresponses/lcm/getAppResponseEmpty.json";
    protected static final String GET_APP_RESPONSE_DUPLICATE_DETAILS = "expectedresponses/lcm/getAppResponseDuplicateDetails.json";
    protected static final String CREATE_APP_RESPONSE_SUCCESS = "expectedresponses/lcm/createAppResponse.json";
    protected static final String CREATE_APP_RESPONSE_INTERNAL_SERVER_ERROR = "expectedresponses/lcm/createAppResponse_InternalServerError.json";
    protected static final String CREATE_APP_RESPONSE_NOT_FOUND_ERROR = "expectedresponses/lcm/createAppResponse_NotFound.json";
    protected static final String LCM_APP_RESPONSE_INVALID_BODY = "expectedresponses/lcm/lcmAppResponse_InvalidBody.json";


    protected UUID setUpDefaultPackageUploadAndCreateJob() throws URISyntaxException, IOException {
        final Path tempFolderPath = Paths.get(tempFolderLocation);
        FileUtils.copyFileToFolder(tempFolderPath, Constants.TEST_CSAR_NAME, Constants.TEST_CSAR_LOCATION);
        return initJobEntity();
    }

    protected UUID initJobEntity(final OnboardingJobStatus status) {
        final OnboardingJobEntity onboardingJobEntity = onboardingJobRepository
            .save(OnboardingJobEntity.builder().fileName(Constants.TEST_CSAR_NAME).status(status).build());

        return onboardingJobEntity.getId();
    }

    protected UUID initJobEntity() {
        return initJobEntity(OnboardingJobStatus.UPLOADED);
    }

    protected OnboardingEventEntity getSingleErrorEventFromJob(final OnboardingJobEntity onboardingJobEntity) {
        final Set<OnboardingEventEntity> events = onboardingJobEntity.getOnboardingEventEntities();
        final List<OnboardingEventEntity> errorEvents = events.stream().filter(event -> event.getType() == OnboardingEventType.ERROR).toList();
        return errorEvents.get(0);
    }

}
