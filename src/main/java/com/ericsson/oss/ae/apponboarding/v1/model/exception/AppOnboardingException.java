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

package com.ericsson.oss.ae.apponboarding.v1.model.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class AppOnboardingException extends RuntimeException {

    private final int errorCode; // Unknown
    private final HttpStatusCode httpStatus;

    public AppOnboardingException(final String message) {
        super(message);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = -1;
    }

    public AppOnboardingException(final String message, final HttpStatusCode httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = -1;
    }

    public AppOnboardingException(final int errorCode, final String message, final HttpStatusCode httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
