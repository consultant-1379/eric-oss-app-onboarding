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

import java.util.stream.Stream;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApplicationStatusConverter implements AttributeConverter<ApplicationStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(final ApplicationStatus applicationStatus) {
        if (applicationStatus == null) {
            return null;
        }
        return applicationStatus.getCode();
    }

    @Override
    public ApplicationStatus convertToEntityAttribute(final Integer code) {
        if (code == null) {
            return null;
        }

        return Stream.of(ApplicationStatus.values()).filter(c -> c.getCode() == code.intValue()).findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}