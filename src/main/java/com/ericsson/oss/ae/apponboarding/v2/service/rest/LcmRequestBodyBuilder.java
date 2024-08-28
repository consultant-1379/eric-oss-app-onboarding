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

package com.ericsson.oss.ae.apponboarding.v2.service.rest;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.CreateAppRequest;

/**
 * RestHandler for generating request body for sending request.
 */

@Slf4j
@Component
public class LcmRequestBodyBuilder {

    private static final String PROPAGATED_HEADER_KEY = "propagated";

    private static final String PROPAGATED_HEADER_VALUE = "onboarding";

    /**
     * Generates and sends a getApp request without body to a given url.
     *
     * @param url          String Url used to build request.
     * @param httpMethod   The Http method used for the request (GET or DELETE).
     * @param acceptedMediaType Accepted Media Type
     * @return Returns get request response
     */
    public RestRequest buildRequestWithoutBody(final String url, final HttpMethod httpMethod, final MediaType acceptedMediaType) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(acceptedMediaType));
        // Needed for lcm to audit log properly.
        headers.set(PROPAGATED_HEADER_KEY, PROPAGATED_HEADER_VALUE);

        final HttpEntity<String> model = new HttpEntity<>(headers);
        return new RestRequest(url, httpMethod, model);
    }

    public RestRequest buildRequestWithBody(final String url, final CreateAppRequest requestBody,
                                            final HttpMethod httpMethod) {
        final HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        // Needed for lcm to audit log properly.
        headers.set(PROPAGATED_HEADER_KEY, PROPAGATED_HEADER_VALUE);

        final HttpEntity<CreateAppRequest> model = new HttpEntity<>(requestBody,headers);
        return new RestRequest(url, httpMethod, model);
    }
}