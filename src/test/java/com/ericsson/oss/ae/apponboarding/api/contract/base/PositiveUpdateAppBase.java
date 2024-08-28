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

import com.ericsson.oss.ae.api.v1.model.AppOnboardingPutRequestDto;
import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;

import java.util.HashSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

public class PositiveUpdateAppBase extends OnboardingBase {

    public void validate() {

        final Application application = new Application();
        application.setStatus(ApplicationStatus.ONBOARDED);
        application.setName("eric-oss-app-mgr");
        application.setVersion("0.0.0-1");
        application.setArtifacts(new HashSet<>());
        application.setId(1l);

        given(applicationService.updateApplication(eq(1l), any(AppOnboardingPutRequestDto.class))).willReturn(application);

    }
}
