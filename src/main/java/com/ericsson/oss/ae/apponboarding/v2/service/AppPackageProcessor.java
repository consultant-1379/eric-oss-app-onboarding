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

import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.ROLLBACK_FAILED_TO_DELETE_ARTIFACTS_DETAIL;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.ROLLBACK_REMOVED_ALL_ARTIFACTS_DETAIL;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.ONBOARDING_JOB_ROLLBACK_TITLE;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEventType;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v2.exception.DataRequestFailedException;
import com.ericsson.oss.ae.apponboarding.v2.exception.BucketCreationException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingEventRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.RetriableDataAccessService;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.CreateAppRequest;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.lcm.AppDetails;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;

/**
 * Contains logic to process an app package csar archive as part of an onboarding request.
 */
@Service
public class AppPackageProcessor {

    @Autowired
    private DecompressorService decompressorService;

    @Autowired
    private CsarParserService csarParserService;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private AppLcmService appLcmService;

    @Autowired
    private AppOnboardingLegacyService appOnboardingLegacyService;

    @Autowired
    OnboardingJobRepository onboardingJobRepository;

    @Autowired
    OnboardingEventRepository onboardingEventRepository;

    @Autowired
    RetriableDataAccessService retriableDbAccessService;

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;

    @Autowired
    private FileService fileService;

    private static final Logger logger = LoggerFactory.getLogger(AppPackageProcessor.class);

    @Async(Consts.APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME)
    public void processAppPackage(@Nonnull final String fileName, @Nonnull final UUID jobId) {
        logger.info("processAppPackage() processing App package content in a new thread {}", Thread.currentThread());
        OnboardingJobEntity onboardingJobEntity = null;

        try {
            onboardingJobEntity = getOnboardingJobEntity(jobId);

            // Decompress the CSAR package on the filesystem and update state = UNPACKED
            decompressorService.decompressCsarPackage(fileName, jobId);
            onboardingJobEntity = updateOnboardingJobEntity(onboardingJobEntity, OnboardingJobStatus.UNPACKED);
            logger.info("processAppPackage() Package decompressed successfully, Onboarding-Job status = {}", OnboardingJobStatus.UNPACKED);

            // Parse the artifacts for the APP Components and get the internal model for all the package content
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobEntity);
            onboardingJobEntity = updateOnboardingJobEntity(onboardingJobEntity, OnboardingJobStatus.PARSED);
            logger.info("processAppPackage() Package content parsed successfully, Onboarding-Job status = {}", OnboardingJobStatus.PARSED);

            // Check if an App for this package name and version already exists in LCM
            logger.info("processAppPackage() Checking if the App with name {}, version {} and provider {} already exists in App LCM", appRequest.getName(), appRequest.getVersion(), appRequest.getProvider());
            checkIfAppAlreadyExists(appRequest.getName(), appRequest.getVersion(), appRequest.getProvider(), jobId);

            // Store all the artifacts in the object storage. Users will fetch the artifacts from here when needed.
            artifactStorageService.storeArtifacts(onboardingJobEntity, appRequest);
            logger.info("processAppPackage() All package artifacts stored successfully");

            // Create App in LCM
            logger.info("processAppPackage() Sending a request to create the App in App LCM: {}", appRequest);
            final AppDetails appDetails = appLcmService.createApp(appRequest);

            logger.info("processAppPackage() Successfully created App with name {} and id {} in App LCM", appDetails.getName(), appDetails.getId());
            onboardingJobEntity.setAppId(appDetails.getId().toString());
            // Update the Onboarding Job state
            updateOnboardingJobEntity(onboardingJobEntity, OnboardingJobStatus.ONBOARDED);
            logger.info("processAppPackage() package onboarded successfully, Onboarding-Job status = {}", OnboardingJobStatus.ONBOARDED);

        } catch (final BucketCreationException ex) {
            rollbackOnboardingJob(onboardingJobEntity, ex, true);

        } catch (final OnboardingJobException e) {
            rollbackOnboardingJob(onboardingJobEntity, e, false);
        }

        finally {
            //cleanup temporary file/folder created
            cleanUpLocalFileSystem(fileName, jobId);
            logger.info("processAppPackage() cleanup, removed csar {} and temp folder {} created during job execution", fileName, jobId);
        }
        logger.info("processAppPackage() completed onboarding job for app-package: {}, job-id: {}", fileName, jobId);
    }

    private void rollbackOnboardingJob(final OnboardingJobEntity onboardingJobEntity, final OnboardingJobException e, final boolean bucketCreationError) {
        if (onboardingJobEntity != null) {
            handleOnboardingJobCreateFailure(onboardingJobEntity, e.getProblemDetails(), bucketCreationError);
            logger.error("processAppPackage() failed processing onboarding job {} with problem title: {}, and problem detail: {}", onboardingJobEntity.getId(),
                e.getProblemDetails().getTitle(),
                e.getProblemDetails().getDetail(), e);
        }
    }

    private void handleOnboardingJobCreateFailure(final OnboardingJobEntity onboardingJobEntity, final ProblemDetails problemDetails, final boolean bucketCreationError) {
        try {
            if (isRollbackRequiredForJob(onboardingJobEntity)) {
                performRollbackAndUpdateStatus(onboardingJobEntity, problemDetails, bucketCreationError);
            } else {
                updateOnboardingJobEntity(onboardingJobEntity, OnboardingJobStatus.FAILED, problemDetails.getTitle(), problemDetails.getDetail());
            }
        } catch (final DataRequestFailedException ex) {
            logger.error("handleOnboardingJobCreateFailure() Data request failed for onboardingJob {} during rollback. Cannot update the DB with the current state {} of the job.",
                onboardingJobEntity.getId(), onboardingJobEntity.getStatus(), ex);
        }
    }

    private OnboardingJobEntity getOnboardingJobEntity(final UUID jobId) {
        return retriableDbAccessService.findById(jobId).orElseThrow(() -> new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorMessages.PROCESS_CSAR_PROBLEM_TITLE, String.format(ErrorMessages.ONBOARDING_JOB_ENTITY_NOT_FOUND, jobId)));
    }

    private void checkIfAppAlreadyExists(final String appName, final String appVersion, final String appProvider, final UUID jobId) {
        final List<AppDetails> appDetailsList = appLcmService.getApp(appName, appVersion);
        if ((appDetailsList != null) && appDetailsList.stream().anyMatch(app -> app.getProvider() == null ||
            app.getProvider().isEmpty() || app.getProvider().equalsIgnoreCase(appProvider))) {
            logger.error("checkIfAppAlreadyExists() failed processing onboarding job {} due to duplicate app package in v3 App-LCM", jobId);
            throw new OnboardingJobException(HttpStatus.CONFLICT, ErrorMessages.CREATE_ONBOARDING_JOB_PROBLEM_TITLE,
                ErrorMessages.DUPLICATE_APP_EXISTS_DETAIL_APP_LCM);
        }

        /*
            To be deprecated. Only required while both v1 and v2 apis are coexisting. Should be
            deleted on deprecation.
         */
        final List<Application> legacyApps = appOnboardingLegacyService.getLegacyApps(appName, appVersion);
        if (legacyApps != null && !legacyApps.isEmpty()) {
            logger.error("checkIfAppAlreadyExists() failed processing onboarding job {} due to duplicate app package in v1 app-onboarding", jobId);
            throw new OnboardingJobException(HttpStatus.CONFLICT, ErrorMessages.CREATE_ONBOARDING_JOB_PROBLEM_TITLE,
                ErrorMessages.DUPLICATE_APP_EXISTS_DETAIL_APP_ONBOARDING);
        }

    }

    private OnboardingJobEntity updateOnboardingJobEntity(final OnboardingJobEntity onboardingJobEntity, @Nonnull final OnboardingJobStatus status) {
        return updateOnboardingJobEntity(onboardingJobEntity, status, null, null);
    }

    private OnboardingJobEntity updateOnboardingJobEntity(final OnboardingJobEntity onboardingJobEntity, @Nonnull final OnboardingJobStatus status,
                                                          @Nullable final String failureTitle, @Nullable final String failureDetail) {
        final String currentStatus = onboardingJobEntity.getStatus().getValue();
        final String newStatus = status.getValue();
        if (currentStatus.equals(OnboardingJobStatus.UPLOADED.getValue())
                && newStatus.equals(OnboardingJobStatus.UNPACKED.getValue())) {
            onboardingJobEntity.setStatus(OnboardingJobStatus.UNPACKED);
        } else if (currentStatus.equals(OnboardingJobStatus.UNPACKED.getValue())
                && newStatus.equals(OnboardingJobStatus.PARSED.getValue())) {
            onboardingJobEntity.setStatus(OnboardingJobStatus.PARSED);
        } else if (currentStatus.equals(OnboardingJobStatus.PARSED.getValue())
            && newStatus.equals(OnboardingJobStatus.ROLLBACK_FAILED.getValue())) {
            onboardingJobEntity.setStatus(OnboardingJobStatus.ROLLBACK_FAILED);
            onboardingJobEntity.setEndTimestamp(Timestamp.from(Instant.now()));
            createOnbordingEvent(OnboardingEventType.ERROR, onboardingJobEntity, failureTitle, failureDetail);
        } else if (currentStatus.equals(OnboardingJobStatus.PARSED.getValue())
            && newStatus.equals(OnboardingJobStatus.ONBOARDED.getValue())) {
            onboardingJobEntity.setStatus(OnboardingJobStatus.ONBOARDED);
            onboardingJobEntity.setEndTimestamp(Timestamp.from(Instant.now()));
        } else {
            onboardingJobEntity.setStatus(OnboardingJobStatus.FAILED);
            onboardingJobEntity.setEndTimestamp(Timestamp.from(Instant.now()));
            createOnbordingEvent(OnboardingEventType.ERROR, onboardingJobEntity, failureTitle, failureDetail);
        }

        return retriableDbAccessService.saveOnboardingJob(onboardingJobEntity);
    }

    private void performRollbackAndUpdateStatus(final OnboardingJobEntity job, final ProblemDetails problemDetails, final boolean bucketCreationError)
    {
        try {
            logger.info("performRollbackAndUpdateStatus() Rolling back onboarding job id {}", job.getId());
            if (!bucketCreationError) {
                artifactStorageService.deleteArtifacts(job.getId());
                createOnbordingEvent(OnboardingEventType.INFO, job, ONBOARDING_JOB_ROLLBACK_TITLE, ROLLBACK_REMOVED_ALL_ARTIFACTS_DETAIL);
            } else {
                logger.info("performRollbackAndUpdateStatus() no artifacts stored for job id {}",job.getId());
            }
            updateOnboardingJobEntity(job, OnboardingJobStatus.FAILED, problemDetails.getTitle(), problemDetails.getDetail());
            logger.info("performRollbackAndUpdateStatus() Successfully rolled back onboarding job id {}", job.getId());
        }
        catch (final DataRequestFailedException ex) {
            logger.error("performRollbackAndUpdateStatus() Request to persist the onboarding data for job {} in the db has failed. Current job status is {}. Error Detail: {} ",
                job.getId(), job.getStatus(), ex.getProblemDetails().getDetail(), ex);
        }
        catch(final OnboardingJobException e)
        {
            logger.error("performRollbackAndUpdateStatus() onboarding-job rollback error - failed to delete artifact for job id: {}. Reason: {}", job.getId(), e.getProblemDetails().getDetail(), e);
            // Update state for the job to ROLLBACK_FAILED and create an event with the reason for the original onboarding failure
            updateOnboardingJobEntity(job,OnboardingJobStatus.ROLLBACK_FAILED, problemDetails.getTitle(), problemDetails.getDetail());
            // Create an additional event for the error that occurred when rolling back the onboarded artifacts
            createOnbordingEvent(OnboardingEventType.ERROR, job, ONBOARDING_JOB_ROLLBACK_TITLE, ROLLBACK_FAILED_TO_DELETE_ARTIFACTS_DETAIL);
        }
    }

    private void createOnbordingEvent(final OnboardingEventType eventType, final OnboardingJobEntity onboardingJobEntity, final String eventTitle, final String eventDetail) {
       retriableDbAccessService.saveOnboardingEvent(
           OnboardingEventEntity.builder()
           .onboardingJobEntity(onboardingJobEntity)
           .type(eventType)
           .title(eventTitle)
           .detail(eventDetail)
           .build());
    }

    /**
     * Determines if rollback is required for a given OnboardingJobEntity.
     *
     * Rollback is only required when the status of the entity is PARSED. States prior to PARSED
     * have nothing to roll back, and the state following PARSED is the final state ONBOARDED,
     * which also does not require rollback.
     *
     * @param entity The OnboardingJobEntity to check.
     * @return true if the entity's status is PARSED, or false if not
     */
    private boolean isRollbackRequiredForJob(final OnboardingJobEntity entity) {
        return entity.getStatus().getValue().equals(OnboardingJobStatus.PARSED.getValue());
    }

    private void cleanUpLocalFileSystem(final String fileToRemove, final UUID jobId) {
        fileService.removeFileFromLocalFilesystem(fileToRemove);

        if (jobId != null) {
            fileService.removeFolderFromLocalFilesystem(jobId.toString());
        }
    }
}
