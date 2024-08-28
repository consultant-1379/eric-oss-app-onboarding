/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

package com.ericsson.oss.ae.apponboarding.v1.controller.jobs;

import com.ericsson.oss.ae.apponboarding.v1.service.jobs.EnvironmentVariableCreator;
import io.fabric8.kubernetes.api.model.EnvVar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class EnvironmentVariableCreatorTest {

    @InjectMocks private EnvironmentVariableCreator environmentVariableCreator;

    @Test
    public void testEnvironmentVariableThatDoesntExist() {
        EnvVar response = environmentVariableCreator.createEnvVarFromEnv("test", "invalidSecret");
        Assertions.assertTrue(response.getValue() == null);
    }
}
