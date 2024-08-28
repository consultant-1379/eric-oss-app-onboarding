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

package com.ericsson.oss.ae.apponboarding.v2.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;
import com.ericsson.oss.ae.apponboarding.v1.service.ApplicationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This service class is responsible for cross-checking applications
 * against the v1 onboarding process and the ACM-R solution.
 * It ensures that the same application cannot be onboarded to both v1 and ACM-R solutions.
 *
 * Deprecated - This class should be removed once the deprecation of the v1
 * functionality is in progress. It serves as a temporary measure to ensure
 * applications are not onboarded to both v1 and ACM-R solutions.
 */
@Service
@Slf4j
@AllArgsConstructor
public class AppOnboardingLegacyService {

    @Autowired
    ApplicationService applicationService;

    /**
     * Retrieves a list of applications with the specified name, version, and onboarding status.
     *
     * @param name the name of the application
     * @param version the version of the application
     * @return a list of applications that match the specified name, version, and have the status 'ONBOARDED'
     */
    public List<Application> getLegacyApps(final String name, final String version)
    {
        return applicationService.findAppsWithNameAndVersionAndStatus(name, version, ApplicationStatus.ONBOARDED);
    }
}
