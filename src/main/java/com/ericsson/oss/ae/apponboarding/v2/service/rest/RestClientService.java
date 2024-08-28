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

import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import com.ericsson.oss.ae.apponboarding.v2.exception.RestRequestException;
import com.ericsson.oss.ae.apponboarding.v2.exception.RestRequestRetryException;

public interface RestClientService {

    /**
     * Call rest end point
     *
     * @param requestBody - request body for sending rest request
     * @return ResponseEntity
     */
    @Retryable(retryFor = {RestRequestRetryException.class},
            maxAttemptsExpression = "#{${lcmServiceRetry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${lcmServiceRetry.delay}}"))
    ResponseEntity<String> callRestEndpoint(final RestRequest requestBody);

    /**
     * Recovery method if all the spring retry fails
     *
     * @param ex          - RestClientException details
     * @param requestBody - request body for sending rest request
     */
    @Recover
    ResponseEntity<String> handleFailedRequest(final RestRequestException ex, final RestRequest requestBody);
}