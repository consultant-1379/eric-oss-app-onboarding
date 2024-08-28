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

package com.ericsson.oss.ae.apponboarding.v1.service;

import com.ericsson.oss.ae.apponboarding.v1.model.entity.Artifact;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public interface ArtifactRegistry {

    String download(final Long id) throws IOException;

    void delete(Artifact artifact);

}
