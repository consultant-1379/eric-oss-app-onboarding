/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

/**
 * Exception thrown to indicate a failure to create a bucket for artifacts in Object Store .
 * Can occur when there is an error either during executing a pre-check on bucket existence
 * before creating a bucket, or during actual creation of the bucket.
 */
public class BucketCreationException extends OnboardingJobException {

    public BucketCreationException(final HttpStatus status, final String title, final String detail) {
        super(status, title, detail);
    }
}
