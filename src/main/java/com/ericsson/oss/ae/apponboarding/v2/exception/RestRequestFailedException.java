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

package com.ericsson.oss.ae.apponboarding.v2.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception to handle failure for a REST request
 */
@Getter
public class RestRequestFailedException extends RestRequestException {
    public RestRequestFailedException(HttpStatus status, String errorDetails) {
        super(status, errorDetails);
    }
}
