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

package com.ericsson.oss.ae.apponboarding.v2.utils;

import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_SIZE;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_TYPE_RAPP;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.APP_PACKAGE_VENDOR_ERICSSON;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.ONBOARDING_JOB_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

import com.ericsson.oss.ae.apponboarding.api.v2.model.AppLink;
import com.ericsson.oss.ae.apponboarding.api.v2.model.AppPackageResponse;
import com.ericsson.oss.ae.apponboarding.api.v2.model.LinksHref;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpStatus;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobLink;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

import io.minio.MinioClient;

public class TestUtils {
    private static final String OBJECT_STORE_ACCESS_KEY = "object-store-user";
    private static final String OBJECT_STORE_SECRET_KEY = "object-store-password";
    private static MinioClient minioClient;

    public static OnboardingJob dummyOnboardingJobDto() {

        final LinksHref onboardingJobLink = new LinksHref().href("app-onboarding/v2/onboarding-jobs/" + ONBOARDING_JOB_ID.toString());
        final AppLink appLink = new AppLink()
            .id("61db75f4-89dc-41b8-b597-a0979d85eb58")
            .href("app-lifecycle-management/v3/apps/61db75f4-89dc-41b8-b597-a0979d85eb58");

        return new OnboardingJob().id(ONBOARDING_JOB_ID).packageVersion("1.1.0")
                .vendor(APP_PACKAGE_VENDOR_ERICSSON).type(APP_PACKAGE_TYPE_RAPP).packageSize(APP_PACKAGE_SIZE).status(OnboardingJobStatus.ONBOARDED)
                .onboardStartedAt(Instant.now().toString()).self(onboardingJobLink).app(appLink);
    }

    public static AppPackageResponse dummyAppPackageResponse() {
        return new AppPackageResponse()
            .onboardingJob(new OnboardingJobLink()
                .id(ONBOARDING_JOB_ID)
                .href("app-onboarding/v2/onboarding-jobs/" + ONBOARDING_JOB_ID.toString()))
            .fileName("eric-oss-hello-world-app-1.0.0-1.csar");
    }

    /**
     * Create a folder on windows c drive if not already there. For unit test execution.
     *
     * @throws IOException
     */
    public static void createDirForCsarUpload(final String filePath) throws IOException {
        // When running on local windows laptop
        if (System.getProperty("os.name").contains("Windows")) {
            Files.createDirectories(Paths.get("C:" + filePath));
        }
    }

    public static void assertInternalServerError(final OnboardingJobException exception, final String errorMsg, final String... params) {
        final String expectedMessage = (params != null && params.length > 0) ? String.format(errorMsg, (Object[]) params) : errorMsg;
        Assertions.assertTrue(exception.getProblemDetails().getDetail().contains(expectedMessage));
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getResponseStatus());
    }

    /**
     * Init object store container.
     *
     * @return the object store container
     */
    public static ObjectStoreContainer initObjectStoreContainer() {
        final ObjectStoreContainer objectStoreContainer = new ObjectStoreContainer(OBJECT_STORE_ACCESS_KEY, OBJECT_STORE_SECRET_KEY);
        objectStoreContainer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(objectStoreContainer::stop));

        // The ObjectStoreContainer's mapped port is not fixed and changes
        // each time a new minio docker container is started. For integration
        // tests we need to set the mapped port in the minioClient, and it is set as a
        // system property to override the fixed port value 9000 configured in the
        // properties yaml file
        final int mappedPort = objectStoreContainer.getPort();
        System.setProperty("object-store.port", String.valueOf(mappedPort));

        return objectStoreContainer;
    }

}