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

package com.ericsson.oss.ae.apponboarding.api.contract.base;

import static org.mockito.BDDMockito.given;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

public class NegativeGetJobByIdNotFoundBase extends OnboardingBase {

    @Override
    public void validate() {
        String jobId = "26471181-1d14-4119-9724-326111122232";
        given(onboardingJobsService.getOnboardingJobById(UUID.fromString(jobId))).willThrow(
            new OnboardingJobException(HttpStatus.NOT_FOUND, String.format(ErrorMessages.ONBOARDING_JOB_ENTITY_NOT_FOUND, jobId)));
    }
}
