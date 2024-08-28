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

package com.ericsson.oss.ae.apponboarding.v1.model.entity;

public enum ApplicationStatus {

    FAILED(10), UPLOADED(20), UNPACKED(30), PARSED(40), ONBOARDED(50), DELETING(60);

    private int code;

    ApplicationStatus(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
