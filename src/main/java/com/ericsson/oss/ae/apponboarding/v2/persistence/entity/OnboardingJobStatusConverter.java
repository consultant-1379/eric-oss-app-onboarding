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

package com.ericsson.oss.ae.apponboarding.v2.persistence.entity;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import jakarta.persistence.AttributeConverter;

public class OnboardingJobStatusConverter implements AttributeConverter<OnboardingJobStatus, String> {
    @Override
    public String convertToDatabaseColumn(OnboardingJobStatus attribute) {
        return attribute.name();
    }

    @Override
    public OnboardingJobStatus convertToEntityAttribute(String dbData) {
        return OnboardingJobStatus.valueOf(dbData);
    }
}
