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

package com.ericsson.oss.ae.apponboarding.v2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
public class MinIOClientConfig {

    @Value("${object-store.hostname}")
    private String hostname;
    @Value("${object-store.port}")
    private int port;
    @Value("${object-store.credentials.username}")
    private String username;
    @Value("${object-store.credentials.password}")
    private String password;

    @Bean(name = "minioClient")
    public MinioClient buildClient() {
        final MinioClient.Builder minioClientBuilder = MinioClient.builder();
        return minioClientBuilder.endpoint(hostname, port, false)
            .credentials(username, password)
            .build();
    }
}
