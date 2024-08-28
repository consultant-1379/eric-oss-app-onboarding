/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

import org.springframework.http.HttpStatus;
import org.springframework.validation.Errors;

public class AppOnboardingValidationException extends AppOnboardingException {

    private final transient Errors errors;

    public AppOnboardingValidationException(String message, HttpStatus status, Errors errors) {
        super(message, status);
        this.errors = errors;
    }

    public Errors getErrors() {
        return errors;
    }
}
