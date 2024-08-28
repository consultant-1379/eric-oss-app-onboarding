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

package com.ericsson.oss.ae.apponboarding.v2.utils;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;

/**
 * Manages the MinIO docker container needed for package onboarding integration tests.
 * <p>
 * In the integration tests, we need to simulate inter-microservice interactions - this class spins up a TestContainers copy of the ADP Object Store
 * MinIO image.
 */
public class ObjectStoreContainer extends GenericContainer<ObjectStoreContainer> {
    private static final String DEFAULT_IMAGE = "armdocker.rnd.ericsson.se/proj-adp-eric-data-object-storage-mn-drop/eric-data-object-storage-mn:1.19.0-5";
    private static final int MINIO_SERVICE_PORT = 9000;
    private static final String MINIO_ROOT_USER = "MINIO_ROOT_USER";
    private static final String MINIO_ROOT_PASSWORD = "MINIO_ROOT_PASSWORD";
    private static final String MINIO_CONFIG_ENV_FILE = "MINIO_CONFIG_ENV_FILE";
    private static final String MINIO_HEALTH_READY_ENDPOINT = "/minio/health/ready";
    private static final String MINIO_STORAGE_DIRECTORY = "/tmp/data";

    private final String accessKey;
    private final String secretKey;

    /**
     * Instantiates a new Object store container.
     *
     * @param accessKey
     *            the access key
     * @param secretKey
     *            the secret key
     */
    public ObjectStoreContainer(final String accessKey, final String secretKey) {
        super(DEFAULT_IMAGE);
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        withNetworkAliases("minio-" + Base58.randomString(6));
        addExposedPort(MINIO_SERVICE_PORT);
        withPrivilegedMode(true);
        withEnv(MINIO_ROOT_USER, accessKey);
        withEnv(MINIO_ROOT_PASSWORD, secretKey);

        // Docker Hub MinIO image has this variable - ADP MinIO doesn't
        // Setting it to the same as in Docker Hub image so it starts OK
        withEnv(MINIO_CONFIG_ENV_FILE, "config.env");

        withCommand("minio", "server", MINIO_STORAGE_DIRECTORY);

        setWaitStrategy(
                new HttpWaitStrategy().forPort(MINIO_SERVICE_PORT).forPath(MINIO_HEALTH_READY_ENDPOINT).withStartupTimeout(Duration.ofMinutes(3)));
    }

    /**
     * Gets host address.
     *
     * @return the host address
     */
    public String getHostAddress() {
        return getContainerIpAddress();
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {
        return getMappedPort(MINIO_SERVICE_PORT);
    }
}
