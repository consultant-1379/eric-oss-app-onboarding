/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.ae.apponboarding.v2.persistence.repository;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEventType;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingEventEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class OnboardingEventRepositoryTest {

    @Autowired
    OnboardingEventRepository eventRepository;

    @Autowired
    OnboardingJobRepository jobRepository;

    @Transactional
    @Test
    public void testFindById_success() {
        OnboardingEventEntity event = eventRepository.save(RepositoryTestUtils.dummyOnboardingEvent());

        final OnboardingEventEntity eventResponse = eventRepository.findById(event.getId()).orElse(null);

        Assertions.assertNotNull(eventResponse);
        Assertions.assertEquals(event.getId(), eventResponse.getId());
        Assertions.assertEquals(OnboardingEventType.INFO, eventResponse.getType()); 
        Assertions.assertEquals(event.getTitle(), eventResponse.getTitle());
        Assertions.assertNotNull(eventResponse.getTimestamp());
    }

    @Transactional
    @Test
    public void testFindById_successWithOnboardingJob() {
        OnboardingJobEntity jobToSave = RepositoryTestUtils.dummyOnboardingJob();
        jobToSave.setOnboardingEventEntities(null);
        OnboardingJobEntity job = jobRepository.save(jobToSave);
        OnboardingEventEntity eventToSave = RepositoryTestUtils.dummyOnboardingEvent();
        eventToSave.setOnboardingJobEntity(job);
        OnboardingEventEntity event = eventRepository.save(eventToSave);

        final OnboardingEventEntity eventResponse = eventRepository.findById(event.getId()).orElse(null);

        Assertions.assertNotNull(eventResponse);
        Assertions.assertNotNull(eventResponse.getOnboardingJobEntity());
        Assertions.assertEquals(event.getId(), eventResponse.getId());
        Assertions.assertEquals(job.getId(), eventResponse.getOnboardingJobEntity().getId());
    }

    @Transactional
    @Test
    public void testFindById_noRecord() {
        eventRepository.save(RepositoryTestUtils.dummyOnboardingEvent());

        final OnboardingEventEntity eventResponse = eventRepository.findById(UUID.randomUUID()).orElse(null);

        Assertions.assertNull(eventResponse);
    }

    @Transactional
    @Test
    public void testSave_defaultTimestamp() {
        OnboardingEventEntity event = RepositoryTestUtils.dummyOnboardingEvent();

        OnboardingEventEntity eventSaved = eventRepository.save(event);
        Assertions.assertNotNull(eventSaved);
        Assertions.assertNotNull(eventSaved.getTimestamp());
    }
}
