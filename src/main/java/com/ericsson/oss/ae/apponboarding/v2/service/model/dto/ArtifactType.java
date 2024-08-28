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

package com.ericsson.oss.ae.apponboarding.v2.service.model.dto;

/**
 * This is an enum representing types of Artifacts.
 */
public enum ArtifactType {
    /**
     * Represents helm chart artifact packaged as .tgz.
     */
    HELM,
    /**
     * Represents tar file containing images referenced by the helm chart.
     */
    IMAGE,
    /**
     * Represents artifact type whose use is transparent or has not been defined or checked by app-mgr.
     */
    OPAQUE
}
