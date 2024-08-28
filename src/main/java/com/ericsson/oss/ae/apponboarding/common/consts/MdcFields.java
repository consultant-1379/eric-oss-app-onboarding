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

package com.ericsson.oss.ae.apponboarding.common.consts;

import lombok.Getter;

/**
 * Represents the MDC fields that can be used across the application.
 *
 * MDC lets the developer place information in a diagnostic context that can be subsequently retrieved
 * by certain logback components. The MDC manages contextual information on a per-thread basis. Without
 * MDC we can only populate the message field of logs, by using MDC we can specify our own fields e.g.: "subject".
 */
@Getter
public enum MdcFields {
    FACILITY("facility"),
    RESPONSE_CODE("resp_code"),
    RESPONSE_MESSAGE("resp_message"),
    SUBJECT("subject"),
    CATEGORY("category"),
    DST_TRACE_ID("traceId"),
    DST_SPAN_ID("spanId");

    private final String value;

    MdcFields(String value) {
        this.value = value;
    }
}
