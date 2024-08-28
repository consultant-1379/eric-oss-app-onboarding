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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ericsson.oss.ae.apponboarding.api.v2.model.AppLink;
import com.ericsson.oss.ae.apponboarding.api.v2.model.LinksHref;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobItems;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;

import org.modelmapper.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.modelmapper.ModelMapper;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;

@Component
public class OnboardingJobMapper {

    private final ModelMapper modelMapper;

    @Autowired
    private final UrlGenerator urlGenerator;

    public OnboardingJobMapper() {
        this.urlGenerator = new UrlGenerator();
        this.modelMapper = new ModelMapper();
        configureMapping();
    }

    private void configureMapping() {
        Converter<Timestamp, String> utcConverter = ctx -> ctx.getSource() == null ? null : ctx.getSource().toInstant().toString();

        modelMapper.createTypeMap(OnboardingJobEntity.class, OnboardingJob.class)
            .addMappings(mapper -> mapper.using(utcConverter).map(OnboardingJobEntity::getStartTimestamp, OnboardingJob::setOnboardStartedAt))
            .addMappings(mapper -> mapper.using(utcConverter).map(OnboardingJobEntity::getEndTimestamp, OnboardingJob::setOnboardEndedAt))
            // adding custom converter to sort the events by date and time, oldest first
            .addMappings(mapper -> mapper.using((new OnboardingEventEntityConverter()))
                .map(OnboardingJobEntity::getOnboardingEventEntities, OnboardingJob::setEvents));
    }

    public OnboardingJob toOnboardingJob(OnboardingJobEntity entity) {
        final OnboardingJob onboardingJob = modelMapper.map(entity, OnboardingJob.class);
        onboardingJob.setSelf(buildSelfLink(entity.getId()));
        onboardingJob.setApp(buildAppLink(entity.getAppId()));
        return onboardingJob;
    }

    public OnboardingJobItems toOnboardingJobList(List<OnboardingJobEntity> entityList) {
        List<OnboardingJob> onboardingJobList = new ArrayList<>();

        for (final OnboardingJobEntity onboardingJobEntity : entityList) {
            OnboardingJob tempOnboardingJob;

            tempOnboardingJob = modelMapper.map(onboardingJobEntity, OnboardingJob.class);
            tempOnboardingJob.setSelf(buildSelfLink(onboardingJobEntity.getId()));
            tempOnboardingJob.setApp(buildAppLink(onboardingJobEntity.getAppId()));
            onboardingJobList.add(tempOnboardingJob);
        }
        return new OnboardingJobItems().items(onboardingJobList);
    }

    private LinksHref buildSelfLink(final UUID onboardedJobId) {
        final LinksHref selfLink = new LinksHref();
        selfLink.setHref(urlGenerator.generateOnboardedJobLinkForExternalClient(String.valueOf(onboardedJobId)));
        return selfLink;
    }

    private AppLink buildAppLink(final String appId) {
        final AppLink appLink = new AppLink();
        if (appId != null && !appId.isEmpty()) {
            appLink.setId(appId);
            appLink.setHref(urlGenerator.generateAppLinkForExternalClient(appId));
        }
        return appLink;
    }
}
