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
import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;
import lombok.Getter;

@Getter
public class BucketAlreadyExistsException extends RuntimeException {

    private final HttpStatus responseStatus;
    private final transient ProblemDetails problemDetails;


    public BucketAlreadyExistsException(final HttpStatus status, final String title, final String detail) {
	responseStatus = status;
	problemDetails = new ProblemDetails()
	    .title(title)
	    .status(status.value())
	    .detail(detail);
    }
}
