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

public class NegativeGetAppsBadRequestBase extends OnboardingBase {

    public void validate() {
        Mockito.doThrow(new AppOnboardingException("Bad Request", HttpStatus.BAD_REQUEST)).when(applicationService)
            .findAll("name2:eric-oss-app-mgr", null, null, null);
        Mockito.doThrow(new AppOnboardingException("Bad Request", HttpStatus.BAD_REQUEST)).when(applicationService).findAll(null, "test", null, null);
        Mockito.doThrow(new AppOnboardingException("Bad Request", HttpStatus.BAD_REQUEST)).when(applicationService).findAll(null, null, "-1", null);
        Mockito.doThrow(new AppOnboardingException("Bad Request", HttpStatus.BAD_REQUEST)).when(applicationService).findAll(null, null, null, "-1");
        Mockito.doThrow(new AppOnboardingException("Bad Request", HttpStatus.BAD_REQUEST)).when(applicationService).findAll(null, null, "1", null);
    }
}
