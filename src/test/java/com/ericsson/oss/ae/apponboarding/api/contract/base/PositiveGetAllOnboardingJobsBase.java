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

import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobItems;

public class PositiveGetAllOnboardingJobsBase extends OnboardingBase {
    protected static final String GET_ALL_ONBOARDING_JOBS_CONTENT = "contracts/v2/get/positive/getAllOnboardingJobs/response.json";

    @Override
    public void validate() {
        // For Get all jobs, no query provided
        final Map<String, String> emptyQuery = new HashMap<>();
        given(onboardingJobsService.getOnboardingAllJobsList(emptyQuery, null, null, null)).willReturn(getOnboardingJobsResponsePayload());

        // For GET with specific query on the fileName property
        final Map<String, String> queryParam = new HashMap<>();
        queryParam.put("fileName", "eric-oss-hello-world-app.csar");
        given(onboardingJobsService.getOnboardingAllJobsList(queryParam, null, null, null))
            .willReturn(getOnboardingJobsResponsePayload());

        // For GET with query, but no matching jobs for the response
        final Map<String, String> queryParamNotExistingFile = new HashMap<>();
        queryParamNotExistingFile.put("fileName", "eric-oss-not-onboarded-app.csar");
        given(onboardingJobsService.getOnboardingAllJobsList(queryParamNotExistingFile, null, null, null))
            .willReturn(new OnboardingJobItems().items(new ArrayList<>()));
    }

    private OnboardingJobItems getOnboardingJobsResponsePayload() {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(getClasspathResourceAsString(GET_ALL_ONBOARDING_JOBS_CONTENT), new TypeReference<OnboardingJobItems>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
