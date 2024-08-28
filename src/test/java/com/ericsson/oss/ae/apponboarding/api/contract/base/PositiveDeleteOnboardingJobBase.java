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

import static org.mockito.Mockito.doNothing;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;

import java.util.UUID;

public class PositiveDeleteOnboardingJobBase extends OnboardingBase {
    @Override
    public void validate() {
        doNothing().when(onboardingJobsService).deleteOnboardingJob(UUID.fromString("26471a81-1de4-4ad9-9724-326eefd22230"));
    }
}

