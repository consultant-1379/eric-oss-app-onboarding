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

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

import java.util.UUID;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

public class NegativeDeleteOnboardingJobBadRequestBase extends OnboardingBase {
    @Override
    public void validate() {
        Mockito.doThrow(new OnboardingJobException(HttpStatus.BAD_REQUEST, "An error occurred when processing request")).when(onboardingJobsService).deleteOnboardingJob(UUID.fromString("26471a81-1de4-4ad9-9724-326eefd22231"));
    }
}
