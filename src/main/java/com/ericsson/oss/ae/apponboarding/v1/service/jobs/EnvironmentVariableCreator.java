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

package com.ericsson.oss.ae.apponboarding.v1.service.jobs;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * A utility class for Jobs to create environment variables.
 */
@Service
public class EnvironmentVariableCreator {

    /**
     * Extracts a secret and creates an environment variable containing the secret
     *
     * @param name
     *     - The name to be assigned to the environment variable
     * @param secretName
     *     - The name of the secret to be retrieved from environment
     * @param secretKey
     *     - They key of the secret to be retrieved from environment
     * @return EnvVar - An environment variable that will hold the secret information
     */
    public EnvVar createEnvVarFromSecret(final String name, final String secretName, final String secretKey) {
        return new EnvVarBuilder().withName(name).withValueFrom(
                new EnvVarSourceBuilder().withSecretKeyRef(new SecretKeySelectorBuilder().withName(secretName).withKey(secretKey).build()).build())
            .build();
    }

    /**
     * Creates an environment variable from the current environment and creates an environment variable object from that data
     *
     * @param targetEnv
     *     - The name given to new environment variable object
     * @param sourceEnv
     *     - The environment variable to be retrieved
     * @return EnvVar - The environment object that contains the retrieved value of the wanted environment variable
     */
    public EnvVar createEnvVarFromEnv(final String targetEnv, final String sourceEnv) {
        return createEnvVarFromString(targetEnv, System.getenv(sourceEnv));
    }

    /**
     * Create an environment variable from a name and value
     *
     * @param targetEnv
     *     - The name given to new environment variable object
     * @param value
     *     -  The value given to new environment variable object
     * @return EnvVar - The environment object that contains the constructed environment variable
     */
    public EnvVar createEnvVarFromString(final String targetEnv, final String value) {
        return new EnvVarBuilder().withName(targetEnv).withValue(value).build();
    }

    public EnvVar createEnvFromValue(final String name, final String value) {
        final EnvVar envVar1 = new EnvVar();
        envVar1.setName(name);
        envVar1.setValue(value);
        return envVar1;
    }

    public String createValueFromEnvVar(final String name, final String defaultValue) {
        return Optional.ofNullable(System.getenv(name)).orElse(defaultValue);
    }
}
