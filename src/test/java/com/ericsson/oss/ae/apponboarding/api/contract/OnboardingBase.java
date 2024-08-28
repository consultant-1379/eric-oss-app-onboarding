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

package com.ericsson.oss.ae.apponboarding.api.contract;

import com.ericsson.oss.ae.apponboarding.v1.controller.ApplicationController;
import com.ericsson.oss.ae.apponboarding.common.controller.advice.ExceptionControllerAdvice;
import com.ericsson.oss.ae.apponboarding.v1.service.ApplicationService;
import com.ericsson.oss.ae.apponboarding.v1.service.ArtifactService;
import com.ericsson.oss.ae.apponboarding.v2.controller.OnboardingJobsController;
import com.ericsson.oss.ae.apponboarding.v2.service.OnboardingJobsService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)

public abstract class OnboardingBase {

    // V1 Controller & Service
    @Mock public ApplicationService applicationService;
    @InjectMocks public ApplicationController onboardingApiController;

    @Mock public ArtifactService artifactService;

    // V2 Controller & Service
    @Mock public OnboardingJobsService onboardingJobsService;
    @InjectMocks public OnboardingJobsController onboardingJobsController;

    // Common V1 & V2 Exception Controller
    @InjectMocks public ExceptionControllerAdvice exceptionControllerAdvice;

    @BeforeEach
    public void setup() {
        // Mock MVC for V1 and V2 controller
        final StandaloneMockMvcBuilder standaloneMockMvcBuilder = MockMvcBuilders.standaloneSetup(onboardingApiController, onboardingJobsController)
            .setControllerAdvice(exceptionControllerAdvice);

        RestAssuredMockMvc.standaloneSetup(standaloneMockMvcBuilder);
        validate();
    }

    public abstract void validate();
}
