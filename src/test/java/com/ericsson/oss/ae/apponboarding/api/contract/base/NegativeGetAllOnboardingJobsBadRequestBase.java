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

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.QUERY_PARAM_INVALID_ID_FORMAT_ERR_MSG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.springframework.http.HttpStatus;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

public class NegativeGetAllOnboardingJobsBadRequestBase extends OnboardingBase {

    @Override
    public void validate() {
        given(onboardingJobsService.getOnboardingAllJobsList(any(), eq(null), eq("-2"), eq(null))).willThrow(
            new OnboardingJobException(HttpStatus.BAD_REQUEST, QUERY_PARAM_INVALID_ID_FORMAT_ERR_MSG));
    }
}
