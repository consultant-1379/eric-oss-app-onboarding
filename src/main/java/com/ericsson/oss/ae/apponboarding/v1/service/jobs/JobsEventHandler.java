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

package com.ericsson.oss.ae.apponboarding.v1.service.jobs;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationEvent;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;
import com.ericsson.oss.ae.apponboarding.v1.service.ApplicationService;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

@Configuration
public class JobsEventHandler {

    private final KubernetesClient kubernetesClient;

    private Watch jobWatcher;
    @Autowired ApplicationService applicationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsEventHandler.class);

    public JobsEventHandler(final KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;

    }

    @Scheduled(initialDelayString = "#{${deployment.cron.initialDelay}}", fixedRateString = "#{${deployment.cron.fetchRate}}")
    public void performScheduledTask() {
        LOGGER.debug("performScheduledTask() Starting cron cleanup");
        List<Application> allApps = applicationService.findAllUploadedApplications();
        Set<String> ongoingApps = getOngoingApps().stream().map(job -> job.getMetadata().getLabels().get("appId")).collect(Collectors.toSet());
        for (Application app : allApps) {
            if (!ongoingApps.contains(app.getId().toString())) {
                LOGGER.debug("performScheduledTask() Deleting app as no jobs exist that are processing this app anymore");

                applicationService.setApplicationToDeleting(app);
                applicationService.deleteApplication(app.getId());
                LOGGER.debug("performScheduledTask() App with id {} deleted", app.getId());
            }

        }
    }

    @PostConstruct
    public void jobWatcher() {
        LOGGER.info("Jobs event detector is enabled");

        this.jobWatcher = this.kubernetesClient.batch().v1().jobs().watch(new Watcher<>() {
            @Override
            public void eventReceived(final Action action, final Job job) {
                onEvent(action, job);
            }

            @Override
            public void onClose(final WatcherException e) {
                // will be ignored
            }
        });

    }

    @EventListener(ApplicationReadyEvent.class)
    public void pollForOngoingJobs() {
        List<Job> ongoingJobs = getOngoingApps();
        LOGGER.info("Total Kubernetes jobs prefixed with 'job-on-board-main' that are ongoing at startup: {}", ongoingJobs.size());
        for (Job job : ongoingJobs) {
            LOGGER.debug("Attempting to cleanup job {}, adding to watchlist", job.getMetadata() == null ? null : job.getMetadata().getName());
            JobsService.jobs.add(job);
            onEvent(Watcher.Action.MODIFIED, job);
        }
    }

    @NotNull
    private List<Job> getOngoingApps() {
        List<Job> ongoingJobs = this.kubernetesClient.batch().v1().jobs().list().getItems();
        LOGGER.debug("Total Kubernetes jobs ongoing at startup: {}", ongoingJobs.size());
        ongoingJobs = ongoingJobs.stream().filter(job -> job.getMetadata().getName().contains("job-on-board-main")).collect(Collectors.toList());
        return ongoingJobs;

    }

    @PreDestroy
    public void shutdown() {
        this.jobWatcher.close();
    }

    private void onEvent(final Watcher.Action action, final Job job) {
        final List<Job> jobs = JobsService.jobs;
        final boolean present = jobs.stream().anyMatch(item -> item.getMetadata().getName().equalsIgnoreCase(job.getMetadata().getName()));
        if (present && (action.equals(Watcher.Action.ERROR) || action.equals(Watcher.Action.MODIFIED))) {
            final List<JobCondition> conditions = job.getStatus().getConditions();
            final boolean isComplete = conditions.stream().anyMatch(cond -> ("Complete".equalsIgnoreCase(cond.getType())));
            final boolean isFailed = conditions.stream().anyMatch(cond -> ("Failed".equalsIgnoreCase(cond.getType())));
            deleteCompletedOrFailedJob(isFailed, isComplete, job, jobs);
        }
    }

    private void deleteCompletedOrFailedJob(final boolean isFailed, final boolean isComplete, final Job job, final List<Job> jobs){
        if (isFailed || isComplete) {
            final Long appId = Long.parseLong(job.getMetadata().getLabels().get("appId"));
            final Application appWithFinishedState = applicationService.findApplicationEvents(appId);
            //completed jobs can have failed for runtime issues, failed jobs are for deployment issues
            //adding this log purely for app staging debugging for now, can be removed at a later date
            if (appWithFinishedState != null && appWithFinishedState.getEvents() != null && !appWithFinishedState.getEvents().isEmpty()) {
                if (appWithFinishedState.getEvents().toString().contains("ERROR")) {
                    LOGGER.error("onEvent() Application failed during execution, associated events are: {}", appWithFinishedState.getEvents());
                }
                if (isFailed) {
                    updateJobForFailure(appWithFinishedState);
                }
            }
            LOGGER.info("onEvent() Deleting completed job {} in namespace {}", job.getMetadata().getName(), job.getMetadata().getNamespace());
            jobs.removeIf(job1 -> job1.getMetadata().getName().equalsIgnoreCase(job.getMetadata().getName()));
            final String namespace = kubernetesClient.getNamespace();
            this.kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(job.getMetadata().getName()).delete();
        }

    }

    private void updateJobForFailure(Application appWithFailedJob) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplication(appWithFailedJob);
        event.setDate(new Date());
        event.setType(" Error ");
        event.setText("The app has failed to onboard while processing/unpacking the artifacts");
        appWithFailedJob.setStatus(ApplicationStatus.FAILED);
        appWithFailedJob.getEvents().add(event);
        applicationService.saveApplication(appWithFailedJob);
        LOGGER.error("updateJobForFailure() Created new event on app due to failure");
    }
}