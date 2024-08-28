/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Artifact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ArtifactService {

    @Autowired private ArtifactRepository artifactRepository;
    @Autowired private ArtifactRegistryFactory artifactRegistryFactory;

    public List<Artifact> findArtifactsByLocation(List<String> locations) {
        return artifactRepository.findByLocationIn(locations);
    }

    public Optional<Artifact> getArtifactsById(Long applicationId, Long artifactId) {
        return artifactRepository.findByApplicationIdAndId(applicationId, artifactId);
    }

    public void deleteArtifact(Artifact artifact) {
        artifactRegistryFactory.getRegistry(artifact.getType().getRegistryComponentName()).delete(artifact);
    }
}
