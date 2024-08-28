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

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEventType;
import jakarta.persistence.AttributeConverter;

public class OnboardingEventTypeConverter implements AttributeConverter<OnboardingEventType, String> {
    @Override
    public String convertToDatabaseColumn(OnboardingEventType attribute) {
        return attribute.name();
    }

    @Override
    public OnboardingEventType convertToEntityAttribute(String dbData) {
        return OnboardingEventType.valueOf(dbData);
    }
}
