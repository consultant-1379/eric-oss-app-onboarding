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

import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Generic exception to capture error details related to OnboardingJob use cases.
 */
@Getter
public class OnboardingJobException extends RuntimeException {
    
    private final HttpStatus responseStatus;
    private final transient ProblemDetails problemDetails;

    /**
     * New OnboardingJobException with customised title field. Used for populating Onboarding Event response objects.
     * The type field is used to distinguish between INFO and ERROR types for Onboarding Events.
     * This field should not be used for REST responses to the Onboarding User.
     * @param status
     * @param title  Only used for populating OnboardingEvent title field
     * @param detail
     */
    public OnboardingJobException(final HttpStatus status, final String title, final String detail) {
        responseStatus = status;
        problemDetails = new ProblemDetails()
            .title(title)
            .status(status.value())
            .detail(detail);
    }

    /**
     * New OnboardingJobException where title is populated from the Status value
     * @param status
     * @param detail
     */
    public OnboardingJobException(final HttpStatus status, final String detail) {
        responseStatus = status;
        problemDetails = new ProblemDetails()
            .title(status.getReasonPhrase())
            .status(status.value())
            .detail(detail);
    }
}

