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
import jakarta.persistence.EntityNotFoundException;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class NegativeGetArtifactNotFoundBase extends OnboardingBase {
    @Mock private ArtifactRepository artifactRepository;

    public void validate() {
        when(applicationService.getArtifacts(anyLong())).thenThrow(EntityNotFoundException.class);
        when(applicationService.findById(anyLong())).thenThrow(EntityNotFoundException.class);
        when(artifactRepository.getById(anyLong())).thenThrow(EntityNotFoundException.class);
        when(artifactRepository.findById(anyLong())).thenThrow(EntityNotFoundException.class);
        when(artifactRepository.findByApplicationIdAndId(anyLong(), anyLong())).thenThrow(EntityNotFoundException.class);
    }
}
