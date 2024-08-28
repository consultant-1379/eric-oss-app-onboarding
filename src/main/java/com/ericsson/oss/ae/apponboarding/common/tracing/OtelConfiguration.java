/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.ae.apponboarding.common.tracing;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;

@Configuration
public class OtelConfiguration {

    private final Environment environment;

    @Autowired
    public OtelConfiguration(final Environment environment) {
        this.environment = environment;
    }

    @Bean
    @ConditionalOnExpression("${ericsson.tracing.enabled} && 'grpc'.equals('${ericsson.tracing.exporter.protocol}')")
    /**
     * This function is for use grpc (port 4317) channel for span export.
     */
    public OtlpGrpcSpanExporter otlpExporterGrpc() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(environment.getProperty(Consts.TRACING_ENDPOINT, Consts.TRACING_ENDPOINT_GRPC))
                .build();
    }

    @Bean
    @ConditionalOnExpression("${ericsson.tracing.enabled} && 'http'.equals('${ericsson.tracing.exporter.protocol}')")
    /**
     * This function is for use http (port 4318) channel for span export.
     */
    public OtlpHttpSpanExporter otlpExporterHttp() {
        return OtlpHttpSpanExporter.builder()
                .setEndpoint(environment.getProperty(Consts.TRACING_ENDPOINT, Consts.TRACING_ENDPOINT_HTTP))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ericsson.tracing", name = "enabled", havingValue = "true")
    /**
     * This function is to pass spans across jaeger UI.
     */
    public JaegerRemoteSampler jaegerRemoteSampler() {
        return JaegerRemoteSampler.builder()
                .setEndpoint(environment.getProperty(Consts.TRACING_JAEGER_ENDPOINT, Consts.TRACING_JAEGER_ENDPOINT_GRPC))
                .setPollingInterval(Duration.ofSeconds(Long.parseLong(environment.getProperty(Consts.TRACING_POLING,
                        Consts.TRACING_POLING_INTERVAL))))
                .setInitialSampler(Sampler.alwaysOff())
                .setServiceName(environment.getProperty("SERVICE_ID", "unknown_service"))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ericsson.tracing", name = "enabled", havingValue = "true")
    ObservationRegistryCustomizer<ObservationRegistry> skipActuatorEndpointsFromObservation() {
        PathMatcher pathMatcher = new AntPathMatcher("/");
        return registry -> registry.observationConfig().observationPredicate((name, context) -> {
            if (context instanceof ServerRequestObservationContext observationContext) {
                return !pathMatcher.match("/actuator/**", observationContext.getCarrier().getRequestURI());
            } else {
                return true;
            }
        });
    }

}
