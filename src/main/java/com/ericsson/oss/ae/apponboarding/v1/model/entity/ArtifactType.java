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

import java.util.Locale;

public enum ArtifactType {

    HELM("H"), IMAGE("I");

    private String code;

    ArtifactType(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getRegistryComponentName() {
        return this.name().toLowerCase(Locale.ENGLISH) + "ArtifactRegistry";
    }

}
