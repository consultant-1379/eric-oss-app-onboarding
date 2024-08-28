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
import jakarta.persistence.EntityNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class NegativeUpdateAppNotFoundBase extends OnboardingBase {

    public void validate() {
        when(applicationService.updateApplication(anyLong(), any(AppOnboardingPutRequestDto.class))).thenThrow(EntityNotFoundException.class);
    }
}
