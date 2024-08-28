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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.service.AppLcmService;
import com.ericsson.oss.ae.apponboarding.v2.service.OnboardingJobsService;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.lcm.AppDetails;


/**
 * A scheduled job that identifies hanging jobs based on certain criteria and updates their state
 * to a final state (FAILED, ONBOARDED, ROLLBACK_FAILED).
 */
@Component
public class DataCleanupScheduler {
    @Autowired private OnboardingJobsService onboardingJobsService;

    @Autowired private AppLcmService appLcmService;

    @Value("${deployment.dataCleanup.schedule.hangingJobAgeMinutes:10}")
    private int timeOffsetMinutes;

    @Value("${deployment.instanceName}")
    private String instanceName;

    private String firstInstance = "eric-oss-app-onboarding-0";

    private static final Logger LOGGER = LoggerFactory.getLogger(DataCleanupScheduler.class);

    /**
     * Periodically checks for onboarding jobs that are considered hanging based on their
     * status (UNPACKED, PARSED, or UPLOADED) and how long they have been in that status.
     * The age of the job that is considered hanging is defined by timeOffsetMinutes and is set
     * in application properties.
     * For jobs found in the PARSED status, an additional verification is performed
     * against App LCM to decide the desired new state for the job.
     * Jobs in other hanging statuses are marked as FAILED.
     *
     * This method is scheduled to run at a fixed rate, with an initial delay, both configured
     * via application properties.
     */
    @Scheduled(initialDelayString = "${deployment.dataCleanup.schedule.initialDelay:60000}",
        fixedRateString = "${deployment.dataCleanup.schedule.fixedRate:600000}")
    public void cleanupHangingJobs() {
        if(instanceName.equals(firstInstance)) {
            LOGGER.debug("cleanupHangingJobs() Checking for hanging jobs");
            final Timestamp tenMinutesAgo = Timestamp.from(Instant.now().minus(timeOffsetMinutes, ChronoUnit.MINUTES));

            final List<OnboardingJobEntity> hangingJobs = onboardingJobsService.getOnboardingJobs(List.of(OnboardingJobStatus.UNPACKED, OnboardingJobStatus.PARSED, OnboardingJobStatus.UPLOADED), tenMinutesAgo);

            LOGGER.debug("cleanupHangingJobs() Found {} jobs that are hanging", hangingJobs.size());
            for (OnboardingJobEntity hangingJob : hangingJobs) {
                if (hangingJob.getStatus() == OnboardingJobStatus.PARSED) {
                    verifyOnboardingJobStatusFromLcm(hangingJob);
                    onboardingJobsService.updateOnboardingJob(hangingJob);
                } else {
                    LOGGER.info("cleanupHangingJobs() Updating job {} to FAILED status", hangingJob.getId());
                    hangingJob.setStatus(OnboardingJobStatus.FAILED);
                    onboardingJobsService.updateOnboardingJob(hangingJob);
                }
            }
        }

    }


    /**
     * This method checks if the application corresponding to the
     * onboarding job exists in App LCM and returns the appropriate {@code OnboardingJobStatus}
     * based on the outcome.
     *
     * If the application corresponding to the hanging job is found in App LCM, the
     * onboarding process has been successfully completed, this method returns {@code ONBOARDED}.
     * If the application is not found, the onboard has failed at any point during the PARSED state.
     * It returns {@code ROLLBACK_FAILED} as the safest state to return to at this point.
     *
     * @param hangingJob The {@code OnboardingJobEntity} representing the job to verify.
     * @return The {@code OnboardingJobStatus} indicating whether the job should be marked as
     *         ONBOARDED or ROLLBACK_FAILED based on the check towards App LCM.
     */
    private void verifyOnboardingJobStatusFromLcm(final OnboardingJobEntity hangingJob) {
        final List<AppDetails> appDetailsList = appLcmService.getApp(hangingJob.getAppName(), hangingJob.getPackageVersion());
        final AppDetails foundApp = findAppDetails(hangingJob.getProvider(), appDetailsList);
        if (foundApp!=null) {
            LOGGER.info("verifyOnboardingJobStatusFromLcm() Updating job {} to ONBOARDED status", hangingJob.getId());
            hangingJob.setStatus(OnboardingJobStatus.ONBOARDED);
            hangingJob.setAppId(foundApp.getId().toString());
        }
        else {
            LOGGER.info("verifyOnboardingJobStatusFromLcm() Updating job {} to ROLLBACK_FAILED status", hangingJob.getId());
            hangingJob.setStatus(OnboardingJobStatus.ROLLBACK_FAILED);
        }
    }

    /**
     * This method checks if the app provider value coming from onboarding jobs is not empty
     * then it should verify with App LCM record. If not return the object if there is a record with
     * same name and version.
     *
     * @param appProvider The provider of the application to match.
     * @param appDetailsList A list of AppDetails containing information about applications.
     * @return true if an application with the specified provider exists. false otherwise.
     */
    private static AppDetails findAppDetails(final String appProvider, final List<AppDetails> appDetailsList) {
        if (appDetailsList == null) {
            return null;
        }
        if (appProvider != null && !appProvider.trim().isEmpty()) {
            return appDetailsList.stream()
                .filter(app -> app.getProvider().equalsIgnoreCase(appProvider))
                .findFirst()
                .orElse(null);
        } else {
            return appDetailsList.isEmpty() ? null : appDetailsList.get(0);
        }
    }
}
