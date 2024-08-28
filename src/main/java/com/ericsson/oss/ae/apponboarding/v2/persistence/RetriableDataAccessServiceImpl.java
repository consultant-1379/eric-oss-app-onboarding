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

import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DATABASE_ACCESS_ERROR;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.ericsson.oss.ae.apponboarding.v2.exception.DataRequestFailedException;
import com.ericsson.oss.ae.apponboarding.v2.exception.DataRequestRetryException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingEventRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;

/**
 * Service that provides access to the Onboarding DB for read and write operations, with support for
 * retries when the DB is unavailable.
 *
 */
@Service
public class RetriableDataAccessServiceImpl implements RetriableDataAccessService {

    @Autowired
    OnboardingJobRepository onboardingJobRepository;

    @Autowired
    OnboardingEventRepository onboardingEventRepository;

    private static final Logger logger = LoggerFactory.getLogger(RetriableDataAccessServiceImpl.class);

    @Override
    public Optional<OnboardingJobEntity> findById(final UUID jobId) {
        try {
            return onboardingJobRepository.findById(jobId);
        } catch (final TransactionException | DataAccessException ex) {
            logger.warn("findById() Failure during attempt to read persistent data from the database for jobId {}. Will attempt retry if limit not exceeded. Reason: {}", jobId, ex.getMessage());
            throw new DataRequestRetryException(HttpStatus.INTERNAL_SERVER_ERROR, String.format(DATABASE_ACCESS_ERROR, ex.getMessage()));
        }
    }
    @Override
    public OnboardingJobEntity saveOnboardingJob(final OnboardingJobEntity onboardingJobEntity) {
        try {
            return onboardingJobRepository.saveAndFlush(onboardingJobEntity);
        } catch (final TransactionException | DataAccessException ex) {
            logger.warn("saveToOnboardingJobRepository() Failure during attempt to save onboarding job data in the database for jobId {}. Will attempt retry if limit not exceeded. Reason: {}",
                onboardingJobEntity.getId(), ex.getMessage());
            throw new DataRequestRetryException(HttpStatus.INTERNAL_SERVER_ERROR, String.format(DATABASE_ACCESS_ERROR, ex.getMessage()));
        }
    }

    @Override
    public OnboardingEventEntity saveOnboardingEvent(final OnboardingEventEntity onboardingEventEntity) {
        try {
            return onboardingEventRepository.save(onboardingEventEntity);
        } catch (final TransactionException | DataAccessException ex) {
            logger.warn("saveToOnboardingEventRepository() Failure during attempt to save data in the onboarding event database for jobId {}. Will attempt retry if limit not exceeded. Reason: {}",
                onboardingEventEntity.getOnboardingJobEntity().getId(), ex.getMessage());
            throw new DataRequestRetryException(HttpStatus.INTERNAL_SERVER_ERROR, String.format(DATABASE_ACCESS_ERROR, ex.getMessage()));
        }
    }

    @Override
    public Optional<OnboardingJobEntity> handleFailedFindByIdRequest(final DataRequestRetryException ex, final UUID jobId){
        logger.error("handleFailedFindById() Retry attempts failed to read persistent data from the database for jobId {}. Reason: {}", jobId, ex.getProblemDetails().getDetail(), ex);
        throw new DataRequestFailedException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getProblemDetails().getDetail());
    }


    @Override
    public OnboardingJobEntity handleFailedSaveOnboardingJobRequest(final DataRequestRetryException ex, final OnboardingJobEntity job) {
        logger.error("handleFailedOnboardingJobRequest() Retry attempts failed to save onboarding job data in the database for jobId {}. Reason: {}", job.getId(), ex.getProblemDetails().getDetail(), ex);
        throw new DataRequestFailedException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getProblemDetails().getDetail());
    }

    @Override
    public OnboardingEventEntity handleFailedSaveOnboardingEventRequest(final DataRequestRetryException ex, OnboardingEventEntity onboardingEventEntity) {
        final UUID jobId = onboardingEventEntity.getOnboardingJobEntity().getId();
        logger.error("handleFailedSaveOnboardingEventRequest() Retry attempts failed to save data in the onboarding event database for for jobId {}. Reason: {}", jobId, ex.getProblemDetails().getDetail(), ex);
        throw new DataRequestFailedException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getProblemDetails().getDetail());
    }
}
