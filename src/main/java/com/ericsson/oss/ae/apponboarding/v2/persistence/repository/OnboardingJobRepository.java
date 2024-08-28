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

package com.ericsson.oss.ae.apponboarding.v2.persistence.repository;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.QUERY_FETCH_ALL_ONBOARDING_JOBS;

public interface OnboardingJobRepository  extends JpaRepository<OnboardingJobEntity, UUID>, JpaSpecificationExecutor<OnboardingJobEntity> {
    @NotNull
    @Query(QUERY_FETCH_ALL_ONBOARDING_JOBS)
    @Override
    List<OnboardingJobEntity> findAll();

    @NotNull
    List<OnboardingJobEntity> findByStatusInAndStartTimestampBefore(final List<OnboardingJobStatus> statuses, final Timestamp startTimestamp);
}
