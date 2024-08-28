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
package com.ericsson.oss.ae.apponboarding.v2.service.model.mapper;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEvent;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;

/**
 * Sorts the event entities by time so that the oldest is displayed first, and then maps to a model OnboardingEvent type.
 */
public class OnboardingEventEntityConverter extends AbstractConverter<Set<OnboardingEventEntity>, List<OnboardingEvent>> {

    private final ModelMapper modelMapper;
    final Comparator<OnboardingEventEntity> onboardingEventTimestampComparator = Comparator.comparing(OnboardingEventEntity::getTimestamp);

    public OnboardingEventEntityConverter() {
        this.modelMapper = new ModelMapper();
        configureMapping();
    }

    private void configureMapping() {
        final Converter<Timestamp, String> utcConverter = ctx -> ctx.getSource() == null ? null : ctx.getSource().toInstant().toString();

        modelMapper.createTypeMap(OnboardingEventEntity.class, OnboardingEvent.class)
            .addMappings(mapper -> mapper.using(utcConverter).map(OnboardingEventEntity::getTimestamp, OnboardingEvent::setOccurredAt));
    }

    @Override
    protected List<OnboardingEvent> convert(Set<OnboardingEventEntity> eventEntities) {
        if (eventEntities == null || eventEntities.isEmpty()) {
            return new ArrayList<>();
        }
        return eventEntities.stream().sorted(onboardingEventTimestampComparator)
            .map(eventEntity -> modelMapper.map(eventEntity, OnboardingEvent.class)).collect(Collectors.toList());
    }

}
