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

package com.ericsson.oss.ae.apponboarding.v2.service;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.BUCKET_ALREADY_EXISTS_DETAIL;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.BUCKET_EXISTS_FAILURE_TITLE;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DATABASE_ACCESS_ERROR;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.FAILED_TO_STORE_ARTIFACTS_TITLE;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEventType;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v2.exception.DataRequestFailedException;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.BucketCreationException;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingEventRepository;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;
import com.ericsson.oss.ae.apponboarding.v2.persistence.RetriableDataAccessService;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Artifact;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.CreateAppRequest;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;

/**
 * Handles the storing of APP artifacts in internal repositories
 *
 */
@Service
public class ArtifactStorageService {

    @Autowired
    private ObjectStoreService objectStoreService;

    @Autowired
    private OnboardingEventRepository onboardingEventRepository;

    @Autowired
    RetriableDataAccessService retriableRepositoryAccessService;

    private static final Logger logger = LoggerFactory.getLogger(ArtifactStorageService.class);

    /**
     * Checks and creates the app-management bucket in Object Store.
     * This acts as a fail-safe mechanism if the bucket is not created by MinioBucketInitializer after application is ready.
     *
     */
    public void storeArtifacts(final OnboardingJobEntity job, final CreateAppRequest appRequest) {
        logger.info("storeArtifacts() Starting the artifact storage process for job ID: {}", job.getId());
        boolean bucketAlreadyExists;
        try {
            bucketAlreadyExists = objectStoreService.doesBucketExist();
            if (!bucketAlreadyExists) {
                objectStoreService.createBucket();
            }

        } catch (final OnboardingJobException ex) {
            logger.warn("storeArtifacts() Error checking if bucket exists for job with id: {}", job.getId(), ex);
            throw new BucketCreationException(HttpStatus.INTERNAL_SERVER_ERROR, FAILED_TO_STORE_ARTIFACTS_TITLE, ex.getProblemDetails().getDetail());
        }
        storeArtifactsForComponent(job, appRequest);
    }

    public void deleteArtifacts(final UUID jobId) {
        logger.info("deleteArtifacts() Deleting artifacts in bucket for job id: {}", jobId);
        objectStoreService.deleteObjects(jobId);
    }

    private void storeArtifactsForComponent(final OnboardingJobEntity job, final CreateAppRequest appRequest) {
        final List<Artifact> allArtifacts = collectAllArtifacts(appRequest);
        final int totalArtifacts = allArtifacts.size();
        AtomicInteger currentArtifactCounter = new AtomicInteger(1);

        allArtifacts.parallelStream().forEach(artifact -> {
            objectStoreService.uploadArtifact(job.getId(), artifact);
            try {
                OnboardingEventEntity event = createOnbordingEvent(job,
                    String.format(Consts.PUSH_OBJECT_STORE_SUCCESS_TITLE, currentArtifactCounter.getAndIncrement(), totalArtifacts),
                    String.format(Consts.PUSH_OBJECT_STORE_SUCCESS_DETAIL, artifact.getName()));
                logger.info(
                    "storeArtifactsForAppComponent() successfully created onboarding event for job {} in the DB. Event title: {} , Event detail: {}",
                    job.getId(), event.getTitle(), event.getDetail());
            } catch (final DataRequestFailedException ex) {
                logger.error("storeArtifactsForAppComponent() DataRequestFailedException caught when creating onboarding event for job {} in the DB.",
                    job.getId(), ex);
                throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PROCESS_CSAR_PROBLEM_TITLE,
                    String.format(DATABASE_ACCESS_ERROR, ex.getProblemDetails().getDetail()));
            }
        });
    }

    private List<Artifact> collectAllArtifacts(CreateAppRequest appRequest) {
        final List<Artifact> allArtifacts = new ArrayList<>();
        appRequest.getComponents().forEach(component -> allArtifacts.addAll(component.getArtifacts()));
        return allArtifacts;
    }

    private OnboardingEventEntity createOnbordingEvent(final OnboardingJobEntity onboardingJobEntity, final String title, final String detail) {
        return retriableRepositoryAccessService.saveOnboardingEvent(OnboardingEventEntity.builder().onboardingJobEntity(onboardingJobEntity).type(OnboardingEventType.INFO)
                    .title(title).detail(detail).build());
    }

}
