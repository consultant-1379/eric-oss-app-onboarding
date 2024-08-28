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

package com.ericsson.oss.ae.apponboarding.api.contract.base;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingException;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

public class NegativeDeleteAppMethodNotAllowedBase extends OnboardingBase {

    public void validate() {
        Mockito.doThrow(new AppOnboardingException("Unable to communicate with container registry, as this operation has been disabled",
            HttpStatus.METHOD_NOT_ALLOWED)).when(applicationService).deleteApplication(405l);
    }
}
