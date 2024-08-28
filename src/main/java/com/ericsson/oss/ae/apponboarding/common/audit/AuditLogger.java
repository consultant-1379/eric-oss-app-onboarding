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

import java.net.URI;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.ericsson.oss.ae.apponboarding.common.consts.MdcFields;

/**
 * Responsible for logging the audit logs.
 */
@Component
@Slf4j
public class AuditLogger {
    private static final String LOG_AUDIT_FACILITY = "log audit";

    /**
     * Logs the information needed for audit log, filling also the MDC parameters.
     * @param responseLength The response length.
     * @param statusCode The HTTP status code.
     * @param subject  The identity of the operator or service who performed the operation
     * @param httpMethod The HTTP method.
     * @param uri The URI.
     */
    public void log(final String responseLength, final String statusCode, final String subject, final String httpMethod, final URI uri, final String additionalMessage) {
        try (MDC.MDCCloseable responseMessageCloseable = MDC.putCloseable(MdcFields.RESPONSE_MESSAGE.getValue(), responseLength);
             MDC.MDCCloseable responseCodeCloseable = MDC.putCloseable(MdcFields.RESPONSE_CODE.getValue(), statusCode);
             MDC.MDCCloseable subjectCloseable = MDC.putCloseable(MdcFields.SUBJECT.getValue(), subject);
             MDC.MDCCloseable facilityCloseable = MDC.putCloseable(MdcFields.FACILITY.getValue(), LOG_AUDIT_FACILITY)) {
            log.info("{} {} {}", additionalMessage, httpMethod, uri);
        }
    }
}
