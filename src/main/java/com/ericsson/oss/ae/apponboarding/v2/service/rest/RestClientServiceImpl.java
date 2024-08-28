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

import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.LCM_REQUEST_FAILED_WITH_RETRIES_DETAIL;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;
import com.ericsson.oss.ae.apponboarding.v2.exception.RestRequestException;
import com.ericsson.oss.ae.apponboarding.v2.exception.RestRequestFailedException;
import com.ericsson.oss.ae.apponboarding.v2.exception.RestRequestRetryException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class for Rest Client Service implementation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestClientServiceImpl implements RestClientService {

    @Autowired
    private final RestTemplate restTemplate;

    @Override
    public ResponseEntity<String> callRestEndpoint(final RestRequest restRequest) {
        log.info("callRestEndpoint() Sending request {} {}", restRequest.getRequestMethod(), restRequest.getUrl());
        try {
            return restTemplate.exchange(restRequest.getUrl(), restRequest.getRequestMethod(), restRequest.getRequest(),
                    String.class);
        } catch (final HttpClientErrorException ex) {
            final HttpStatus httpStatus = HttpStatus.valueOf(ex.getStatusCode().value());
            final String lcmErrorMessage = getLcmErrorMessage(ex);
            log.error("callRestEndpoint() Exception when calling the endpoint : {}, response status {}, error detail: {}", restRequest.getUrl(), httpStatus, lcmErrorMessage, ex);
            throw new RestRequestFailedException(httpStatus, lcmErrorMessage);
        } catch (final HttpServerErrorException ex) {
            throwRetryExceptionIfNeeded(ex);
            // No retry needed
            final HttpStatus httpStatus = HttpStatus.valueOf(ex.getStatusCode().value());
            final String lcmErrorMessage = getLcmErrorMessage(ex);
            log.error("callRestEndpoint() Exception when calling the endpoint : {}, response status {}, error detail: {}", restRequest.getUrl(), httpStatus, lcmErrorMessage, ex);
            throw new RestRequestFailedException(httpStatus, lcmErrorMessage);
        } catch (final ResourceAccessException ex) {
            log.error("callRestEndpoint() ResourceAccessException when calling the endpoint : {} error detail: {}", restRequest.getUrl(), ex.getMessage(), ex);
            throw new RestRequestRetryException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> handleFailedRequest(final RestRequestException ex, final RestRequest requestBody) {
        log.info("handleFailedRequest() App LCM request failed. Detail : {}", ex.getMessage());
        throw new RestRequestFailedException(ex.getHttpStatus(), ex.getErrorDetails());
    }

    private String getLcmErrorMessage(final HttpStatusCodeException ex) {
        final String errorResponseBodyString = ex.getResponseBodyAsString();
        if (!errorResponseBodyString.isEmpty()) {
            final ProblemDetails lcmErrorDetails = getLcmErrorDetails(errorResponseBodyString);
            return lcmErrorDetails.getDetail();
        } else {
            return ex.getMessage();
        }
    }

    private ProblemDetails getLcmErrorDetails(final String responseString) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(responseString, ProblemDetails.class);
        } catch (final JsonProcessingException e) {
            // default message
            log.error("Exception thrown while parsing response body from App LCM : {}", e.getMessage(), e);
            ProblemDetails lcmErrorDetails = new ProblemDetails();
            lcmErrorDetails.setDetail(responseString);
            return lcmErrorDetails;
        }
    }

    protected void throwRetryExceptionIfNeeded(final HttpStatusCodeException ex) {
        final List<HttpStatus> retryStatusCodes = List.of(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT);
        final HttpStatusCode statusCode = ex.getStatusCode();
        if (retryStatusCodes.contains(statusCode)) {
            log.info("throwRetryExceptionIfNeeded() Server exception with status code {} caught for request sent to App LCM - initiating retry attempt", statusCode);
            final HttpStatus responseStatus = HttpStatus.valueOf(statusCode.value());
            final String errorMessage = String.format(LCM_REQUEST_FAILED_WITH_RETRIES_DETAIL, responseStatus);
            throw new RestRequestRetryException(HttpStatus.valueOf(ex.getStatusCode().value()), errorMessage);
        }
    }
}
