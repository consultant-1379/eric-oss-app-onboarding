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

package com.ericsson.oss.ae.apponboarding.v2.service.utils;

import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.ObjectStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Handles the creation of app-management bucket in Object Store after the application is ready
 *
 */
@Component
public class MinioBucketInitializer {

    @Autowired
    private ObjectStoreService objectStoreService;

    @Value("${object-store.bucketName}")
    private String bucketName;

    private static final Logger logger = LoggerFactory.getLogger(MinioBucketInitializer.class);

    /**
     * Initializes the object store bucket when the application is ready.
     *
     * This method is executed asynchronously and listens for the ApplicationReadyEvent.
     * In case of an {@link OnboardingJobException}, it retries the operation based on the specified retry configuration.
     *
     * The method checks if the bucket already exists in the object store. If the bucket does not exist, it creates a new bucket.
     *
     * The retry behavior is controlled by the maxAttemptsExpression and delayExpression properties, which are
     * configured in the application's properties file.
     *
     * @throws OnboardingJobException if an error occurs while checking or creating the bucket.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    @Retryable(retryFor = {OnboardingJobException.class}, maxAttemptsExpression = "${object-store.bucketCheckRetry.maxAttempts}", backoff = @Backoff(delayExpression = "${object-store.bucketCheckRetry.delay}"))
    public void initializeBucket() {
        try {
            boolean bucketAlreadyExists = objectStoreService.doesBucketExist();
            if (!bucketAlreadyExists) {
                objectStoreService.createBucket();
                logger.debug("initializeBucket() Created bucket with name {} in Object Store", bucketName);
            } else {
                logger.info("initializeBucket() Bucket with name {} already exists in Object Store", bucketName);
            }
        } catch (final OnboardingJobException ex) {
            logger.error("initializeBucket() Error occurred while checking/creating bucket: {}", ex.getProblemDetails().getDetail());
            throw ex;
        }
    }

}
