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
package com.ericsson.oss.ae.apponboarding.common.audit;

import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogConstants.ADDITIONAL_MESSAGE_KEY;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogConstants.PREFERRED_USERNAME_CLAIM_KEY;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogConstants.UPN_CLAIM_KEY;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogConstants.PROPAGATED_HEADER_KEY;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.GET_METHOD;
import static com.ericsson.oss.orchestration.eo.config.CommonLoggingConstants.BEARER_SCHEME;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import com.ericsson.oss.ae.apponboarding.common.audit.configuration.AuditLogProperties;
import com.ericsson.oss.orchestration.eo.security.JWTDecoder;

/**
 * Responsible for logging audit logs for every request.
 */
@Slf4j
@ControllerAdvice
public class AuditLogResponseAdvice implements ResponseBodyAdvice<Object>, Ordered {

    private final PathPatternParser pathPatternParser = new PathPatternParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    AuditLogProperties auditLogProperties;

    @Autowired
    NativeWebRequest nativeWebRequest;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (shouldGenerateAccessLog(request)) {
            generateAccessLog(body, request, response);
        }

        return body;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    /**
     * Generates access log.
     *
     * @param body the response body
     * @param request the request
     * @param response the response
     */
    private void generateAccessLog(Object body, ServerHttpRequest request, ServerHttpResponse response) {
        int responseLength = 0;
        int statusCode = 0;
        String preferredUsername = "n/av";
        String additionalMessage = "";

        final String preferredUsernameClaim = JWTDecoder.getUsernameFromJWTToken(getTokenValue(request), objectMapper, PREFERRED_USERNAME_CLAIM_KEY, false);
        final String upnClaim = JWTDecoder.getUsernameFromJWTToken(getTokenValue(request), objectMapper, UPN_CLAIM_KEY, false);

        if (Arrays.asList(nativeWebRequest.getAttributeNames(NativeWebRequest.SCOPE_REQUEST)).contains(ADDITIONAL_MESSAGE_KEY)){
            additionalMessage = (String)nativeWebRequest.getAttribute(ADDITIONAL_MESSAGE_KEY, NativeWebRequest.SCOPE_REQUEST);
        }

        if (body != null) {
            try {
                if (body instanceof String s) {
                    responseLength = s.length();
                } else {
                    responseLength = objectMapper.writeValueAsString(body).length();
                }
            } catch (final JsonProcessingException e) {
                log.error("Error while processing JSON response, reason {}", e.getMessage());
            }
        }

        if (response instanceof ServletServerHttpResponse serverHttpResponse) {
            statusCode = serverHttpResponse.getServletResponse().getStatus();
        }

        if (!Objects.equals(preferredUsernameClaim, "")) {
            preferredUsername = preferredUsernameClaim;
        } else if (!Objects.equals(upnClaim, "")){
            preferredUsername = upnClaim;
        }

        auditLogger.log(Integer.toString(responseLength), Integer.toString(statusCode), preferredUsername, request.getMethod().name(),
                request.getURI(), additionalMessage);
    }

    private String getTokenValue(final ServerHttpRequest request) {
        final String customHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(customHeader) && customHeader.startsWith(BEARER_SCHEME)) {
            return customHeader.substring(7);
        }
        return null;
    }

    /**
     * Checks the request path against the list of paths that must skip access
     * log generation.
     *
     * @param request to be matched.
     * @return {@code true} if access log must be generated for the ongoing request.
     */
    private boolean shouldGenerateAccessLog(final ServerHttpRequest request) {
        return auditLogProperties.getIncludePattern().stream().map(this.pathPatternParser::parse)
                .anyMatch(patternToInclude -> checkPathMatch(patternToInclude, request.getURI().getRawPath()))
                && request.getHeaders().getFirst(PROPAGATED_HEADER_KEY) == null && (!request.getMethod().name().equals(GET_METHOD));
    }

    private boolean checkPathMatch(final PathPattern pattern, final String requestedPath) {
        final PathContainer pathContainer = PathContainer.parsePath(requestedPath);
        return pattern.matches(pathContainer);
    }
}
