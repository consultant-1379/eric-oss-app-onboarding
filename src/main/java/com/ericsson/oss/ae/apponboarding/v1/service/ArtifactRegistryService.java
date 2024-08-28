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

import com.ericsson.oss.ae.apponboarding.common.consts.utils.StringUtils;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Artifact;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ArtifactRegistryService {

    @Autowired private ArtifactRegistryFactory artifactRegistryFactory;
    @Autowired private ArtifactRepository artifactRepository;

    public String downloadArtifact(final Long applicationId, final Long id) throws IOException {
        final Artifact artifact = artifactRepository.getReferenceById(id);
        validate(applicationId, artifact);
        return artifactRegistryFactory.getRegistry(artifact.getType().getRegistryComponentName()).download(artifact.getId());
    }

    private void validate(final Long applicationId, final Artifact artifact) {
        if (artifact.getApplication().getId().longValue() != applicationId.longValue()) {
            throw new AppOnboardingException(3500,
                StringUtils.appendWithSpace("Could not found Artifact with id:", artifact.getId(), " in Application, id: ",
                    artifact.getApplication().getId()), HttpStatus.CONFLICT);
        }
        if (artifact.getApplication().getStatus() != ApplicationStatus.ONBOARDED) {
            throw new AppOnboardingException(3500,
                StringUtils.appendWithSpace("Application is not in ONBOARDED status.", "Artifacts cannot be downloaded.", "Current status is:",
                    artifact.getApplication().getStatus()), HttpStatus.CONFLICT);
        }
    }

}
