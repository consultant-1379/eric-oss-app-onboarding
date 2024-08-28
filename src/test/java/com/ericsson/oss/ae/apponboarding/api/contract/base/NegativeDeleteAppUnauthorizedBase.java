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

public class NegativeDeleteAppUnauthorizedBase extends OnboardingBase {
    public void validate() {
        Mockito.doThrow(new AppOnboardingException("Unauthorized to communicate with container registry", HttpStatus.UNAUTHORIZED))
            .when(applicationService).deleteApplication(401l);
    }
}
