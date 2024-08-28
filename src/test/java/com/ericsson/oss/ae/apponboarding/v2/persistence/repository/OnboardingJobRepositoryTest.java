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

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class OnboardingJobRepositoryTest {

    @Autowired
    OnboardingJobRepository jobRepository;

    @Autowired
    OnboardingEventRepository eventRepository;

    @Transactional
    @Test
    public void testGetById_success() {
        OnboardingJobEntity job = jobRepository.save(RepositoryTestUtils.dummyOnboardingJob());

        final OnboardingJobEntity jobResponse = jobRepository.findById(job.getId()).orElse(null);

        Assertions.assertNotNull(jobResponse);
        Assertions.assertEquals(job.getId(), jobResponse.getId());
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, jobResponse.getStatus());
        Assertions.assertTrue(jobResponse.getStartTimestamp().compareTo(Timestamp.from(Instant.now())) <= 0);
    }

    @Transactional
    @Test
    public void testGetById_successWithOnboardingEvent() {
        OnboardingJobEntity jobToSave = RepositoryTestUtils.dummyOnboardingJob();
        jobToSave.setOnboardingEventEntities(null);
        OnboardingJobEntity job = jobRepository.save(jobToSave);
        OnboardingEventEntity eventToSave = RepositoryTestUtils.dummyOnboardingEvent();
        eventToSave.setOnboardingJobEntity(job);
        OnboardingEventEntity event = eventRepository.save(eventToSave);
        job.setOnboardingEventEntities(Set.of(event));

        final OnboardingJobEntity jobResponse = jobRepository.findById(job.getId()).orElse(null);

        Assertions.assertNotNull(jobResponse);
        Assertions.assertNotNull(jobResponse.getOnboardingEventEntities());
        Assertions.assertEquals(job.getId(), jobResponse.getId());
        Assertions.assertEquals(jobResponse.getId(), jobResponse.getOnboardingEventEntities().iterator().next().getOnboardingJobEntity().getId());
        Assertions.assertEquals(OnboardingJobStatus.ONBOARDED, jobResponse.getStatus());
    }

    @Transactional
    @Test
    public void testFindById_successWithTwoOnboardingEvents() {
        OnboardingJobEntity jobToSave = RepositoryTestUtils.dummyOnboardingJob();
        OnboardingEventEntity event1 = RepositoryTestUtils.dummyOnboardingEvent();
        OnboardingEventEntity event2 = RepositoryTestUtils.dummyOnboardingEvent();
        event2.setTitle("Stored 2 out of 2 artifacts");
        event2.setDetail("Uploaded docker.tar");
        jobToSave.setOnboardingEventEntities(Set.of(event1, event2));
        OnboardingJobEntity job = jobRepository.save(jobToSave);

        final OnboardingJobEntity jobResponse = jobRepository.findById(job.getId()).orElse(null);

        Assertions.assertNotNull(jobResponse);
        Assertions.assertNotNull(jobResponse.getOnboardingEventEntities());
        Assertions.assertEquals(2, jobResponse.getOnboardingEventEntities().size());
        Assertions.assertNotNull(jobResponse.getOnboardingEventEntities().iterator().next().getId());
    }

    @Transactional
    @Test
    public void testGetById_noRecord() {
        jobRepository.save(RepositoryTestUtils.dummyOnboardingJob());

        final OnboardingJobEntity jobResponse = jobRepository.findById(UUID.randomUUID()).orElse(null);

        Assertions.assertNull(jobResponse);
    }

    @Transactional
    @Test
    public void testSave_defaultStartTimestamp() {
        OnboardingJobEntity job = RepositoryTestUtils.dummyOnboardingJob();

        OnboardingJobEntity jobSaved = jobRepository.save(job);
        Assertions.assertNotNull(jobSaved);
        Assertions.assertNotNull(jobSaved.getStartTimestamp());
    }

    @Transactional
    @Test
    public void testFindDistinctAll_success() {
        jobRepository.save(RepositoryTestUtils.dummyOnboardingJob());
        jobRepository.save(RepositoryTestUtils.dummyOnboardingJob());

        List<OnboardingJobEntity> jobs = jobRepository.findAll();
        Assertions.assertNotNull(jobs);

        if (jobs.size() >= 2) {
            OnboardingJobEntity job1 = jobs.get(0);
            OnboardingJobEntity job2 = jobs.get(1);

            Assertions.assertTrue(job1.getStartTimestamp().compareTo(job2.getStartTimestamp()) <= 0);
        }
    }
}
