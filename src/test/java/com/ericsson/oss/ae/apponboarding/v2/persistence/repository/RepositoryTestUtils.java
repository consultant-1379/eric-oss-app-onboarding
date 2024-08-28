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

package com.ericsson.oss.ae.apponboarding.v2.persistence.repository;

import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEvent;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEventType;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;

public class RepositoryTestUtils {

    public static OnboardingJobEntity dummyOnboardingJob() {
        return dummyOnboardingJob(OnboardingJobStatus.ONBOARDED);
    }

    public static OnboardingJobEntity dummyOnboardingJob(final OnboardingJobStatus status) {
        return OnboardingJobEntity.builder().id(ONBOARDING_JOB_ID).appId(UUID.randomUUID().toString()).packageVersion(APP_PACKAGE_VERSION)
                .vendor(APP_PACKAGE_VENDOR_ERICSSON).type(APP_PACKAGE_TYPE_RAPP).fileName(APP_PACKAGE_FILE_NAME).packageSize(APP_PACKAGE_SIZE)
                .status(status).onboardingEventEntities(Set.of(dummyOnboardingEvent())).build();
    }

    public static OnboardingJobEntity dummyOnboardingJobWithoutEvents(final OnboardingJobStatus status) {
        return OnboardingJobEntity.builder().id(ONBOARDING_JOB_ID).appId(UUID.randomUUID().toString()).packageVersion(APP_PACKAGE_VERSION)
            .vendor(APP_PACKAGE_VENDOR_ERICSSON).type(APP_PACKAGE_TYPE_RAPP).fileName(APP_PACKAGE_FILE_NAME).packageSize(APP_PACKAGE_SIZE)
            .status(status).build();
    }

    public static OnboardingJobEntity dummyOnboardingJobForQueryAndSort(final OnboardingJobStatus status) {
        return OnboardingJobEntity.builder().id(UUID.randomUUID()).appId(UUID.randomUUID().toString()).packageVersion("2.0.0-1")
            .vendor("OtherVendor").type("gApp").fileName("eric-oss-dummy-gapp").packageSize("500MiB")
            .status(status).onboardingEventEntities(Set.of(dummyOnboardingEvent())).build();
    }

    public static OnboardingEventEntity dummyOnboardingEvent() {
        return OnboardingEventEntity.builder().type(OnboardingEventType.INFO).title("Stored 1 out of 2 artifacts")
                .detail("Uploaded hello-1.0.0-1.tgz").build();
    }

    public static Set<OnboardingEventEntity> dummyOnboardingEvents() {
        final Set<OnboardingEventEntity> eventList = new LinkedHashSet<>();
        final OnboardingEventEntity firstArtifactEvent = OnboardingEventEntity.builder().type(OnboardingEventType.INFO).title("Stored 1 out of 2 artifacts")
            .timestamp(Timestamp.from(Instant.now()))
            .detail("Uploaded docker.tar").build();
        eventList.add(firstArtifactEvent);
        final OnboardingEventEntity nextArtifactEvent = OnboardingEventEntity.builder().type(OnboardingEventType.INFO).title("Stored 2 out of 2 artifacts")
            .timestamp(Timestamp.from(Instant.now().plus(1, ChronoUnit.SECONDS)))
            .detail("Uploaded helm-chart-1.0.0.tgz").build();
        eventList.add(nextArtifactEvent);
        final OnboardingEventEntity errortEvent = OnboardingEventEntity.builder().type(OnboardingEventType.ERROR).title("onboard failed")
            .timestamp(Timestamp.from(Instant.now().plus(2, ChronoUnit.SECONDS)))
            .detail("Error during create app").build();
        eventList.add(errortEvent);
        final OnboardingEventEntity rollbackArtifactEvent = OnboardingEventEntity.builder().type(OnboardingEventType.INFO).title("Rollback event")
            .timestamp(Timestamp.from(Instant.now().plus(3, ChronoUnit.SECONDS)))
            .detail("removed all artifacts from storage").build();
        eventList.add(rollbackArtifactEvent);
        return eventList;
    }
}
