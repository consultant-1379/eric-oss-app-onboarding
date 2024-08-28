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

package com.ericsson.oss.ae.apponboarding;

import com.ericsson.oss.ae.apponboarding.common.consts.utils.StringUtils;
import com.ericsson.oss.ae.apponboarding.common.validation.FilterQueryValidator;
import com.ericsson.oss.ae.apponboarding.common.controller.interceptor.TempFileCleanupInterceptor;
import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.service.ArtifactRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.API_ARTIFACT;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.API_V1_APPS;

/**
 * Core Application, the starting point of the application.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan("com.ericsson.oss.ae.apponboarding.*")
public class CoreApplication {

    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ArtifactRepository artifactRepository;
    @Autowired private ArtifactRegistryService artifactRegistryService;

    @Autowired private RestTemplateBuilder builder;

    @Autowired private FilterQueryValidator filterQueryValidator;

    /**
     * Main entry point of the application.
     *
     * @param args
     *     Command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(CoreApplication.class, args);

    }

    /**
     * Configuration bean for Web MVC.
     *
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer webConfigurer() {
        return new WebMvcConfigurer() {
            @Autowired TempFileCleanupInterceptor tempFileCleanupInterceptor;

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(tempFileCleanupInterceptor).addPathPatterns(StringUtils.append(API_V1_APPS, "/*/", API_ARTIFACT) + "/*/file");
            }
        };
    }

    /**
     * Making a RestTemplate, using the RestTemplateBuilder, to use for consumption of RESTful interfaces.
     *
     * @param restTemplateBuilder
     *     RestTemplateBuilder instance
     * @return RestTemplate
     */
    @Qualifier("helmRegistry")
    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    /**
     * Making a RestTemplate, using the RestTemplateBuilder, to use for consumption of RESTful interfaces.
     *
     * @return RestTemplate
     */

    @Qualifier("imageRegistry")
    @Bean
    public RestTemplate imageRegistryRestTemplate() {
        return builder.build();
    }

}
