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

import org.springframework.http.HttpStatusCode;

public class AppOnboardingServerException extends AppOnboardingException {

    public AppOnboardingServerException(final String message, final HttpStatusCode httpStatus) {
        super(message, httpStatus);
    }

}
