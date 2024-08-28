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

package com.ericsson.oss.ae.apponboarding.api.contract.base;

import com.ericsson.oss.ae.apponboarding.api.contract.OnboardingBase;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.*;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.Optional;
import static org.mockito.Mockito.doReturn;

public class PositiveGetArtifactBase extends OnboardingBase {

    @Mock private ArtifactRepository artifactRepository;

    public void validate() {

        final Application application = new Application();
        application.setStatus(ApplicationStatus.ONBOARDED);
        application.setName("eric-oss-app-mgr");
        application.setVersion("0.0.0-1");
        application.setArtifacts(new HashSet<>());
        application.setId(1l);

        Artifact artifact1 = new Artifact();
        artifact1.setApplication(application);
        artifact1.setVersion("0.0.0-1");
        artifact1.setName("eric-oss-app-mgr");
        artifact1.setType(ArtifactType.HELM);
        artifact1.setStatus(ArtifactStatus.COMPLETED);
        artifact1.setId(1l);
        application.getArtifacts().add(artifact1);

        doReturn(application.getArtifacts()).when(applicationService).getArtifacts(1l);
        doReturn(Optional.of(application)).when(applicationService).findById(1l);
        doReturn(Optional.of(artifact1)).when(artifactService).getArtifactsById(1l, 1l);
        doReturn(Optional.of(artifact1)).when(artifactRepository).findById(1l);
        doReturn(Optional.of(artifact1)).when(artifactRepository).findByApplicationIdAndId(1l, 1l);

    }
}
