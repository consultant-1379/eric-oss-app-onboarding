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

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import io.minio.MinioClient;

@TestConfiguration
public class MinioTestClient {
    private static final String OBJECT_STORE_ACCESS_KEY = "object-store-user";
    private static final String OBJECT_STORE_SECRET_KEY = "object-store-password";

    private static ObjectStoreContainer objectStoreContainer = null;


    /**
     * Init minio client.
     *
     * @return the minio client
     */
    @Bean(name = "minioClient")
    @Primary
    @Lazy
    public MinioClient initMinioClient() {
	if (objectStoreContainer == null) {
	    objectStoreContainer = TestUtils.initObjectStoreContainer();
	}
	final MinioClient.Builder minioBuilder = MinioClient.builder();
	final int mappedPort = objectStoreContainer.getPort();
	return minioBuilder.endpoint("https://" + objectStoreContainer.getHost(), mappedPort, false)
	    .credentials(OBJECT_STORE_ACCESS_KEY, OBJECT_STORE_SECRET_KEY).build();
    }
}
