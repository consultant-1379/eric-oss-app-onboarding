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

package com.ericsson.oss.ae.apponboarding.v2.service.parser;

import java.util.UUID;

import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;

/**
 * Supports parsing any artifact for an APP Component.
 * An App Component could have different Artifact Types in the descriptor file. A separate implementation is required to
 * parse different artifacts as their parsing logic could vary from each other.
 *
 * Any concrete implementation of this interface should implement all the methods and should be marked as @Component or @Service. Any new Artifacts
 * created should be added to the AppComponent.artifacts.
 */
public interface ArtifactParser {
    /**
     * Checks if the parser supports the given artifact type
     */
    boolean isSupported(final String artifactType);

    /**
     * Parse the artifact defined in the provided artifact file
     */
    void parse(final UUID jobId, final String csarName, final String artifactFilePath, final Component component);
}
