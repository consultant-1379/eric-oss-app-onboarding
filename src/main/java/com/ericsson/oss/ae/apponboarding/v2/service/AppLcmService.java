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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.exception.RestRequestFailedException;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.CreateAppRequest;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.lcm.AppDetails;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.lcm.AppItems;
import com.ericsson.oss.ae.apponboarding.v2.service.rest.LcmRequestBodyBuilder;
import com.ericsson.oss.ae.apponboarding.v2.service.rest.RestClientService;
import com.ericsson.oss.ae.apponboarding.v2.service.rest.RestRequest;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles all interaction with the App-LCM microservice.
 */
@Service
@Slf4j
@AllArgsConstructor
public class AppLcmService {

    @Autowired
    private final LcmRequestBodyBuilder lcmRequestBodyBuilder;

    @Autowired
    private final RestClientService restClientService;

    @Autowired
    private final UrlGenerator urlGenerator;

    /**
     * Get App with the given name from App Lcm.
     * Uses query parameters from App Lcm to retrieve apps that match name and version.
     * With the API structure: /v3/apps?name=value&version=value
     *
     * @param name name of app to check in app-lcm if it exists
     * @param version version of app to check in app-lcm if it exists
     */
    public List<AppDetails> getApp(final String name, final String version) {
        log.info("getApp() Sending request to get App with {} and {} in App LCM", name, version);
        final String url = urlGenerator.getAppsQueryByNameAndVersionUrl(name, version);
        try {
            final RestRequest restRequest = lcmRequestBodyBuilder.buildRequestWithoutBody(url, HttpMethod.GET, MediaType.APPLICATION_JSON);
            final ResponseEntity<String> responseEntity = restClientService.callRestEndpoint(restRequest);
            return new ObjectMapper().readValue(responseEntity.getBody(), new TypeReference<AppItems>() {}).getItems();
        } catch (final JsonProcessingException ex) {
            log.error("getApp() Request failure when parsing App LCM response. Reason: {}", ex.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.ERROR_ACCESSING_APP_LCM, ErrorMessages.LCM_RESPONSE_MAPPING_FAILURE_DETAIL);
        } catch (final RestRequestFailedException ex) {
            final String errorMessage = ex.getErrorDetails();
            log.error("getApp() Request failure when calling App LCM to get app details based on name {}. Reason: {}", name, errorMessage, ex);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.ERROR_ACCESSING_APP_LCM, errorMessage);
        }
    }

    /**
     * Create an App in App Lcm, passing all app component data from the onboarded csar
     * @param appRequest
     * @return the LcmApp instance created in App LCM
     */
    public AppDetails createApp(final CreateAppRequest appRequest) {
        log.info("createApp() Sending request to create App {} in App LCM.", appRequest.getName());

        final String url = urlGenerator.generateUrlForCreateApp();
        final RestRequest restRequest = lcmRequestBodyBuilder.buildRequestWithBody(url, appRequest, HttpMethod.POST);

        try {
            final ResponseEntity<String> responseEntity = restClientService.callRestEndpoint(restRequest);
            return new ObjectMapper().readValue(responseEntity.getBody(), AppDetails.class);
        }  catch (final JsonProcessingException ex) {
            log.error("createApp() Request failure when parsing App LCM response. Reason: {}", ex.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.ERROR_ACCESSING_APP_LCM, ErrorMessages.LCM_RESPONSE_MAPPING_FAILURE_DETAIL);
        } catch (final RestRequestFailedException ex) {
            final String errorMessage = ex.getErrorDetails();
            log.error("createApp() Request failure when calling App LCM to Create App. Reason: {}", errorMessage, ex);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.ERROR_ACCESSING_APP_LCM, errorMessage);
        }
    }
}