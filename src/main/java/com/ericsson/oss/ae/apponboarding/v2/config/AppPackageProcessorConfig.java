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

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Profile("!test-with-sync-processing")
public class AppPackageProcessorConfig {
    @Value("${appPackageExecutor.threadsCount}")
    private int appPackageExecutorThreadsCount;

    @Value("${appPackageExecutor.queueCapacity}")
    private int appPackageExecutorQueueCapacity;

    @Bean(name = APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME)
    public Executor appPackageExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(appPackageExecutorThreadsCount);
        executor.setMaxPoolSize(appPackageExecutorThreadsCount);
        executor.setQueueCapacity(appPackageExecutorQueueCapacity);
        executor.setThreadNamePrefix(APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME);

        executor.initialize();

        return executor;
    }
}
