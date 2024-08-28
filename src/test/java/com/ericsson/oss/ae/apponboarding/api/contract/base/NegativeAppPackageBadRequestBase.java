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

import static java.lang.String.format;

import static org.mockito.ArgumentMatchers.any;

import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.PLEASE_ONBOARD_VALID_CSAR;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

public class NegativeAppPackageBadRequestBase extends OnboardingBase {
    @Override
    public void validate() {
        Mockito.doThrow(new OnboardingJobException(HttpStatus.BAD_REQUEST, format(Consts.FILE_TYPE_INVALID, "helloworldBadRequest.yaml") + Consts.STRING_SEPARATOR + PLEASE_ONBOARD_VALID_CSAR)).when(onboardingJobsService).onboardAppPackage(any(MultipartFile.class));
    }
}
