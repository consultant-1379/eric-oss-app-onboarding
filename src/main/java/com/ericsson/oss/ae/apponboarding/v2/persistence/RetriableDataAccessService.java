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

package com.ericsson.oss.ae.apponboarding.v2.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import com.ericsson.oss.ae.apponboarding.v2.exception.DataRequestRetryException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;

/**
 *  Provides a service to access the data in the app-manager DB, with retries supported
 *  for robustness.
 *
 */
public interface RetriableDataAccessService {

    /**
     * Find the persisted entity for the given id.   Failure to access the DB will result in 'maxAttempts' retries.
     *
     * @param jobId the id of the job to find
     * @return the OnboardingJobEntity
     */
    @Retryable(retryFor = { DataRequestRetryException.class},
        maxAttemptsExpression = "#{${dataAccessRetry.maxAttempts}}",
        backoff = @Backoff(delayExpression = "#{${dataAccessRetry.delay}}"))
    Optional<OnboardingJobEntity> findById(final UUID jobId);

    /**
     * Handle the failed attempt for 'find' onboarding job data in the DB. All retries have exhausted without success.
     *
     * @param ex the retry exception containing the details about the failure
     * @param jobId the id of the job to find
     * @return the entity
     */
    @Recover
    Optional<OnboardingJobEntity> handleFailedFindByIdRequest(final DataRequestRetryException ex, final UUID jobId);

    /**
     * Save the given onboardingJobEntity in the DB. Failure to access the DB will result in 'maxAttempts' retries.
     *
     * @param onboardingJobEntity the job entity to save
     * @return the entity
     */
    @Retryable(retryFor = { DataRequestRetryException.class},
        maxAttemptsExpression = "${dataAccessRetry.maxAttempts}",
        backoff = @Backoff(delayExpression = "${dataAccessRetry.delay}"))
    OnboardingJobEntity saveOnboardingJob(final OnboardingJobEntity onboardingJobEntity);

    /**
     * Handle the failed attempt for 'save' onboarding job data in the DB. All retries have exhausted without success.
     *
     * @param ex  the retry exception containing the details about the failure
     * @param onboardingJobEntity the job entity to save
     * @return the entity
     */
    @Recover
    OnboardingJobEntity handleFailedSaveOnboardingJobRequest(final DataRequestRetryException ex, final OnboardingJobEntity onboardingJobEntity);

    /**
     * Save the given onboardingEventEntity in the DB. All retries have exhausted without success.
     *
     * @param onboardingEventEntity the event entity to save
     * @return the entity
     */
    @Retryable(retryFor = { DataRequestRetryException.class},
        maxAttemptsExpression = "${dataAccessRetry.maxAttempts}",
        backoff = @Backoff(delayExpression = "${dataAccessRetry.delay}"))
    OnboardingEventEntity saveOnboardingEvent(final OnboardingEventEntity onboardingEventEntity);

    /**
     * Handle the failed attempt for 'save' onboarding event data in the DB. All retries have exhausted without success.
     *
     * @param ex  the retry exception containing the details about the failure
     * @param onboardingEventEntity the entity to save
     * @return the entity
     */
    @Recover
    OnboardingEventEntity handleFailedSaveOnboardingEventRequest(final DataRequestRetryException ex, final OnboardingEventEntity onboardingEventEntity);
}
