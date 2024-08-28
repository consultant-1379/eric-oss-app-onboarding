/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.ae.apponboarding.v1.dao;

import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.QUERY_FETCH_ALL_APPS;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    @Query(QUERY_FETCH_ALL_APPS)
    @Override
    List<Application> findAll();

    List<Application> findByStatusAndOnboardedDateBefore(final ApplicationStatus status, final Date onboardDate);

    List<Application> findByNameAndVersionAndStatus(final String name, final String version, final ApplicationStatus status);
}
