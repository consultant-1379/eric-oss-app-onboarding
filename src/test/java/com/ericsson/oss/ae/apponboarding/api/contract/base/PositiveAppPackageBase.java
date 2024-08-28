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

import static com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils.dummyAppPackageResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;

public class PositiveAppPackageBase extends OnboardingBase {
    @Override
    public void validate() {
        doReturn(dummyAppPackageResponse()).when(onboardingJobsService).onboardAppPackage(any(MultipartFile.class));
    }
}
