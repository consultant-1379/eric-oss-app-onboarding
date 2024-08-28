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

import static com.ericsson.oss.ae.apponboarding.v2.utils.ResourceLoaderUtils.getClasspathResourceAsString;
import static com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils.dummyOnboardingJobDto;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;

public class PositiveGetJobByIdBase extends OnboardingBase {

    protected static final String GET_ONBOARDING_JOB_CONTENT = "contracts/v2/get/positive/getJobById/response.json";

    @Override
    public void validate() {
        given(onboardingJobsService.getOnboardingJobById(UUID.fromString("26471181-1d14-4119-9724-326111122232"))).willReturn(getOnboardingJobByIdResponsePayload());
    }

    private OnboardingJob getOnboardingJobByIdResponsePayload() {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(getClasspathResourceAsString(GET_ONBOARDING_JOB_CONTENT), new TypeReference<OnboardingJob>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
