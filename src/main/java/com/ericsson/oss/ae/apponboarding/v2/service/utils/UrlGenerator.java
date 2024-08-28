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

package com.ericsson.oss.ae.apponboarding.v2.service.utils;

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FORWARD_SLASH;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.ONBOARDING_JOBS_V2_API;
import static com.ericsson.oss.ae.apponboarding.v2.consts.Consts.APPS;
import static com.ericsson.oss.ae.apponboarding.v2.consts.Consts.APP_LCM_URL_V3;
import static com.ericsson.oss.ae.apponboarding.v2.consts.Consts.HTTP;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for reading service hostnames/ports and generating urls.
 * <p>
 * This class reads the hostnames and ports from the environment. These values are defined in values.yaml.
 */
@Component
@Slf4j
public class UrlGenerator {

    @Value("${APP_LCM_SERVICE_HOSTNAME:eric-oss-app-lcm}") private String appLcmHostname;

    @Value("${APP_LCM_SERVICE_PORT:8080}") private String appLcmPort;

    @Value("${APP_MANAGER_APP_ONBOARDING_ROUTE_PATH:app-onboarding}")
    private String appOnboardingRoutePath;

    @Value("${APP_MANAGER_APP_LCM_ROUTE_PATH:app-lifecycle-management}")
    private String appLcmRoutePath;

    public String getAppsQueryByNameAndVersionUrl(String name, String version) {
        log.debug("getAppsQueryByNameAndVersionUrl() generating GET Apps resource URL for query on App name - {} & version - {}" , name, version);
        final String path =  FORWARD_SLASH + APP_LCM_URL_V3 + FORWARD_SLASH + APPS;
        final String query = "name=" + name +"&version="+ version;

        try {
            return new URI(HTTP, null, appLcmHostname, Integer.parseInt(appLcmPort), path, query, null).toString();
        } catch (final URISyntaxException ex) {
            log.error("getAppsQueryByNameAndVersionUrl() Error generating GET apps URL for query on App name with value - {}. Reason: {}", name, ex.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Error generating GET apps URL for query on App name - %s and version - %s", name, version), ex.getMessage());
        }
    }

    public String generateUrlForCreateApp() {
        log.debug("generateUrlForCreateApp() generating Create App resource URL");
        final String path =  FORWARD_SLASH + APP_LCM_URL_V3 + FORWARD_SLASH + APPS;

        try {
            return new URI(HTTP, null, appLcmHostname, Integer.parseInt(appLcmPort), path, null, null).toString();
        } catch (final URISyntaxException ex) {
            log.error("generateUrlForCreateApp() Error generating URL for Create App request, for reason: {}", ex.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generating URL for Create App request", ex.getMessage());
        }
    }

    /**
     * Composes URL for get onboard job
     *
     * @param onboardJobId onboarded job id
     * @return Returns App Onboarding URL for getting onboarded jobs by id
     */
    public String generateOnboardedJobLinkForExternalClient(final String onboardJobId) {
        final String onboardingBasePath = removeLeadingSlashIfPresent(appOnboardingRoutePath);
        return onboardingBasePath + ONBOARDING_JOBS_V2_API + FORWARD_SLASH + onboardJobId;
    }

    /**
     * Composes URL for get apps (App-LCM)
     *
     * @param appId app id
     * @return Returns App Lcm URL for getting app by id
     */
    public String generateAppLinkForExternalClient(final String appId) {
        final String lcmBasePath = removeLeadingSlashIfPresent(appLcmRoutePath);
        return lcmBasePath + FORWARD_SLASH + APP_LCM_URL_V3 + FORWARD_SLASH + APPS + FORWARD_SLASH + appId;
    }

    private String removeLeadingSlashIfPresent(final String url) {
        return url.startsWith("/") ? url.substring(1) : url;
    }
}