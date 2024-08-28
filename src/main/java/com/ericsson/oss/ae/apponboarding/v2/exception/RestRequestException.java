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
 * General Exception to hold information related to REST request failures
 */
@Getter
public class RestRequestException extends RuntimeException {

    public static final long serialVersionUID = 1L;

    private final String errorDetails;
    private final HttpStatus httpStatus;

    public RestRequestException(final HttpStatus status, final String errorDetails) {
        super(status + " " + errorDetails);
        httpStatus = status;
        this.errorDetails = errorDetails;
    }
}