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

import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.SAVE_TO_FILESYSTEM_ERROR;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

public class NegativeAppPackageInternalServerErrorBase extends OnboardingBase {
    @Override
    public void validate() {
        Mockito.doThrow(new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, format(SAVE_TO_FILESYSTEM_ERROR, "csar", "Filesystem full"))).when(onboardingJobsService).onboardAppPackage(any(MultipartFile.class));
    }
}
