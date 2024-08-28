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

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v2.utils.MinioTestClient;

@ContextConfiguration(classes = MinioTestClient.class)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class OtelConfigurationTest {

    @InjectMocks
    private OtelConfiguration otelConfiguration;

    @Mock
    private Environment environment;

    public static final String TRACING_PROTOCOL = "ericsson.tracing.exporter.protocol";
    public static final String TRACING_HTTP = "http";
    public static final String TRACING_GRPC = "grpc";

    @Test
    void otlpExporterGrpc () {
        when(environment.getProperty(TRACING_PROTOCOL, TRACING_HTTP)).thenReturn(TRACING_GRPC);
        when(environment.getProperty(Consts.TRACING_ENDPOINT, Consts.TRACING_ENDPOINT_GRPC))
                .thenReturn(Consts.TRACING_ENDPOINT_GRPC);
        Assert.assertNotNull(otelConfiguration.otlpExporterGrpc());
    }

    @Test
    void otlpExporterHttp () {
        when(environment.getProperty(TRACING_PROTOCOL, TRACING_GRPC)).thenReturn(TRACING_HTTP);
        when(environment.getProperty(Consts.TRACING_ENDPOINT, Consts.TRACING_ENDPOINT_HTTP))
                .thenReturn(Consts.TRACING_ENDPOINT_HTTP);
        Assert.assertNotNull(otelConfiguration.otlpExporterHttp());
    }

    @Test
    void jaegerRemoteSampler () {
        when(environment.getProperty(Consts.TRACING_JAEGER_ENDPOINT, Consts.TRACING_JAEGER_ENDPOINT_GRPC))
                .thenReturn(Consts.TRACING_JAEGER_ENDPOINT_GRPC);
        when(environment.getProperty(Consts.TRACING_POLING, Consts.TRACING_POLING_INTERVAL))
                .thenReturn(Consts.TRACING_POLING_INTERVAL);
        when(environment.getProperty("SERVICE_ID", "unknown_service"))
                .thenReturn("eric-oss-app-onboarding");
        Assert.assertNotNull(otelConfiguration.jaegerRemoteSampler());
    }

    @Test
    void skipActuatorEndpointsFromObservation () {
        Assert.assertNotNull(otelConfiguration.skipActuatorEndpointsFromObservation());
    }
}
