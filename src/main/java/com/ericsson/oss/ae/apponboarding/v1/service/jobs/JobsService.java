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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.PodAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

@Service
public class JobsService {

    @Autowired private KubernetesClient discoveryClient;

    @Autowired private EnvironmentVariableCreator environmentVariableCreator;

    @Value("${deployment.kubernetes.activeDeadlineSeconds}") private String activeDeadlineSeconds;

    @Value("${deployment.instanceName}") private String instanceName;

    @Value("${deployment.kubernetes.ttlSecondsAfterFinished}") private String ttlSecondsAfterFinished;

    private String storageVolume = "storage-volume";

    private String logShipperVolume = "eric-log-shipper-sidecar-storage-path";

    @Value("storage-volume-${deployment.instanceName}") private String pvcName;

    @Value("eric-log-shipper-sidecar-storage-path-${deployment.instanceName}") private String logShipperPvcName;


    @Value("${deployment.serviceMeshEnabled}") private String serviceMesh;

    @Value("${deployment.tlsEnabled}") private String tlsEnabled;

    @Value("${deployment.serviceMeshUserVolume}") private String smUserVolume;

    @Value("${deployment.serviceMeshVolumeMount}") private String smUserVolumeMount;

    protected static final List<Job> jobs = new CopyOnWriteArrayList<>();
    private static final String DB_CREDENTIALS = "eric-appmgr-data-document-db-credentials";
    private static final String ONBOARDING_JOBS_DB_USER_PWD = "custom-pwd";

    protected final HashMap<String, String> labels = new HashMap<>();
    protected final HashMap<String, String> annotations = new HashMap<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(JobsService.class);
    private static final String SERVICE_MESH_ISTIO_KEY = "sidecar.istio.io/inject";
    private static final String SERVICE_MESH_ISTIO_VALUE = "true";
    private static final String SERVICE_MESH_USER_VOLUME = "sidecar.istio.io/userVolume";
    private static final String SERVICE_MESH_USER_VOLUME_MOUNT = "sidecar.istio.io/userVolumeMount";
    private static final Set LOG_STREAMING_METHOD_DIRECT_OR_DUAL = new HashSet<>(Arrays.asList("direct", "dual"));

    public void createJobs(final List<String> args, final String imageName, final Long appId) {
        final boolean serviceMeshEnabled = Boolean.parseBoolean(serviceMesh);
        final boolean tlsEnable = Boolean.parseBoolean(tlsEnabled);
        final KubernetesClient client = discoveryClient;
        labels.put("jobTag", "onboarding-jobs");
        labels.put("jobType", "onboarding-python-job");
        labels.put("appId", appId.toString());
        ObjectMeta metadata;
        final String jobOnBoardMainName = "job-on-board-main-" + generateRandomString(5);
        if (serviceMeshEnabled) {
            logger.debug("Service Mesh is true adding extra labels/annotations for onboarding main job");
            labels.put(SERVICE_MESH_ISTIO_KEY, SERVICE_MESH_ISTIO_VALUE);
            annotations.put(SERVICE_MESH_ISTIO_KEY, SERVICE_MESH_ISTIO_VALUE);
            annotations.put("proxy.istio.io/config", "{ \"holdApplicationUntilProxyStarts\": true }");

            if (tlsEnable) {
                annotations.put(SERVICE_MESH_USER_VOLUME, smUserVolume);
                annotations.put(SERVICE_MESH_USER_VOLUME_MOUNT, smUserVolumeMount);
                labels.put("eric-appmgr-data-document-db", "true");
            }
            metadata = new ObjectMetaBuilder().withName(jobOnBoardMainName).withLabels(labels).withAnnotations(annotations).build();
        } else {
            metadata = new ObjectMetaBuilder().withName(jobOnBoardMainName).withLabels(labels).build();
        }

        final List<EnvVar> serviceInformation = new ArrayList<>();
        serviceInformation.add(environmentVariableCreator.createEnvFromValue("APP_ID", appId.toString()));
        serviceInformation.add(environmentVariableCreator.createEnvVarFromEnv("SERVICE_ID", "SERVICE_ID"));
        serviceInformation.add(environmentVariableCreator.createEnvFromValue("POD_NAME", jobOnBoardMainName));
        serviceInformation.add(environmentVariableCreator.createEnvVarFromEnv("LOGS_STREAMING_METHOD", "LOGS_STREAMING_METHOD"));
        serviceInformation.add(environmentVariableCreator.createEnvFromValue("LOGS_SEVERITY", getLoggerSeverityLevel()));

        final String namespace = client.getNamespace();
        final Job createdJob = client.batch().v1().jobs().inNamespace(namespace)
                .resource(new JobBuilder().withMetadata(metadata).withSpec(getJobSpec(args, imageName, metadata, serviceInformation)).build()).create();

        jobs.add(createdJob);

        logger.info("createJobs() Created job {} with Id {}", createdJob.getMetadata().getName(), createdJob.getMetadata().getUid());
    }

    private JobSpec getJobSpec(List<String> args, String imageName, ObjectMeta metadata, List<EnvVar> serviceInformation) {
        return new JobSpecBuilder().withBackoffLimit(2).withTtlSecondsAfterFinished(Integer.parseInt(ttlSecondsAfterFinished))
            .withTemplate(getPodTemplateSpec(args, imageName, metadata, serviceInformation)).build();
    }

    private PodTemplateSpec getPodTemplateSpec(List<String> args, String imageName, ObjectMeta metadata, List<EnvVar> serviceInformation) {
        return new PodTemplateSpecBuilder().withSpec(
                        new PodSpecBuilder().withRestartPolicy("Never").withServiceAccountName("eric-oss-app-onboarding-sa")
                                .withActiveDeadlineSeconds(Long.parseLong(activeDeadlineSeconds)).withAffinity(getJobAffinity())
                                .withContainers(Lists.newArrayList(getJobContainer(args, imageName, serviceInformation)))
                                .withVolumes(getVolumes())
                                .withImagePullSecrets(Lists.newArrayList(new LocalObjectReferenceBuilder().withName("k8s-registry-secret").build())).build())
                .withMetadata(metadata).build();
    }

    private List<Volume> getVolumes() {
        String logsStreamingMethod = environmentVariableCreator.createValueFromEnvVar("LOGS_STREAMING_METHOD", "indirect");
        if (LOG_STREAMING_METHOD_DIRECT_OR_DUAL.contains(logsStreamingMethod.toLowerCase(Locale.ENGLISH))) {
            return Lists.newArrayList(getVolume(storageVolume, pvcName), getVolume(logShipperVolume, logShipperPvcName));
        } else {
            return Lists.newArrayList(getVolume(storageVolume, pvcName));
        }
    }

    private Volume getVolume(String volumeName, String pvcName) {
        return new VolumeBuilder().withName(volumeName)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build();
    }

    private Container getJobContainer(List<String> args, String imageName, List<EnvVar> serviceInformation) {
        final List<EnvVar> envs = new ArrayList<>();
        envs.addAll(getDbEnvs());
        envs.addAll(getHelmEnvs());
        String containerName = "job-pod-" + generateRandomString(6);
        envs.add(environmentVariableCreator.createEnvFromValue("CONTAINER_NAME", containerName));
        envs.addAll(serviceInformation);

        return new ContainerBuilder()
                .withEnv(envs)
                .withName(containerName)
                .withImage(imageName)
                .withImagePullPolicy("IfNotPresent")
                .withCommand(Lists.newArrayList("/bin/sh", "-c"))
                .withArgs(args)
                .withVolumeMounts(getVolumeMounts()).build();
    }

    private List<VolumeMount> getVolumeMounts() {
        String logsStreamingMethod = environmentVariableCreator.createValueFromEnvVar("LOGS_STREAMING_METHOD", "indirect");
        if (LOG_STREAMING_METHOD_DIRECT_OR_DUAL.contains(logsStreamingMethod.toLowerCase(Locale.ENGLISH))) {
            return Lists.newArrayList(new VolumeMountBuilder().withName(storageVolume).withMountPath("/tmp").build(),
                    new VolumeMountBuilder().withName(logShipperVolume).withMountPath("/logs").build());
        } else {
            return Lists.newArrayList(new VolumeMountBuilder().withName(storageVolume).withMountPath("/tmp").build());
        }
    }

    private Affinity getJobAffinity() {
        return new AffinityBuilder().withPodAffinity(new PodAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(Lists.newArrayList(
                new PodAffinityTermBuilder().withLabelSelector(new LabelSelectorBuilder().withMatchExpressions(
                    new LabelSelectorRequirementBuilder().withKey("statefulset.kubernetes.io/pod-name").withOperator("In")
                        .withValues(Lists.newArrayList(instanceName)).build()).build()).withTopologyKey("kubernetes.io/hostname").build())).build())
            .build();
    }

    private List<EnvVar> getHelmEnvs() {
        List<EnvVar> envs = new ArrayList<>();
        envs.add(environmentVariableCreator.createEnvVarFromEnv("HELM_REG_URL", "HELM_REG_URL"));
        envs.add(environmentVariableCreator.createEnvVarFromSecret("HELM_REG_USER", "eric-lcm-helm-chart-registry", "BASIC_AUTH_USER"));
        envs.add(environmentVariableCreator.createEnvVarFromSecret("HELM_REG_PASSWORD", "eric-lcm-helm-chart-registry", "BASIC_AUTH_PASS"));
        return envs;
    }

    private List<EnvVar> getDbEnvs() {
        List<EnvVar> envs = new ArrayList<>();
        envs.add(environmentVariableCreator.createEnvVarFromEnv("DB_HOST", "DB_HOST"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("DB_USER", "ONBOARDING_JOBS_DB_USER"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("DB_PORT", "DB_PORT"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("DB_NAME", "DB_NAME"));
        envs.add(environmentVariableCreator.createEnvVarFromSecret("DB_PASSWORD", DB_CREDENTIALS, ONBOARDING_JOBS_DB_USER_PWD));

        envs.add(environmentVariableCreator.createEnvVarFromSecret("CONTAINER_REGISTRY_USER", "eric-oss-app-mgr-container-registry-secret", "name"));
        envs.add(environmentVariableCreator.createEnvVarFromSecret("CONTAINER_REGISTRY_PASSWORD", "eric-oss-app-mgr-container-registry-secret",
            "password"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("CONTAINER_REGISTRY_HOST", "CONT_REG_URL"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("NAMESPACE", "NAMESPACE"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("PYTHON_JOB_IMAGE_NAME", "PYTHON_JOB_IMAGE_NAME"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("TTL_SECONDS_AFTER_FINISHED", "TTL_SECONDS_AFTER_FINISHED"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("PYTHON_JOB_POLLING_TIME", "PYTHON_JOB_POLLING_TIME"));
        envs.add(environmentVariableCreator.createEnvVarFromString("PVC_NAME", pvcName));
        envs.add(environmentVariableCreator.createEnvVarFromString("STORAGE_VOLUME", storageVolume));
        envs.add(environmentVariableCreator.createEnvVarFromString("LOG_SHIPPER_PVC_NAME", logShipperPvcName));
        envs.add(environmentVariableCreator.createEnvVarFromString("LOG_SHIPPER_VOLUME", logShipperVolume));
        envs.add(environmentVariableCreator.createEnvVarFromString("INSTANCE_NAME", instanceName));

        envs.add(environmentVariableCreator.createEnvVarFromEnv("SERVICE_MESH_ENABLED", "SERVICE_MESH_ENABLED"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("TLS_ENABLED", "TLS_ENABLED"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("SM_USER_VOLUME", "SM_USER_VOLUME"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("SM_USER_VOLUME_MOUNT", "SM_USER_VOLUME_MOUNT"));

        envs.add(environmentVariableCreator.createEnvVarFromEnv("APP_LCM_SERVICE_HOSTNAME", "APP_LCM_SERVICE_HOSTNAME"));
        envs.add(environmentVariableCreator.createEnvVarFromEnv("APP_LCM_SERVICE_PORT", "APP_LCM_SERVICE_PORT"));
        envs.add(environmentVariableCreator.createEnvVarFromString("APP_MANAGER_APP_LCM_ROUTE_PATH", "APP_MANAGER_APP_LCM_ROUTE_PATH"));
        return envs;
    }

    private String generateRandomString(final int targetStringLength) {
        final int leftLimit = 97; // letter 'a'
        final int rightLimit = 122; // letter 'z'
        final Random random = new SecureRandom();

        return random.ints(leftLimit, rightLimit + 1).limit(targetStringLength)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

    }

    private String getLoggerSeverityLevel() {
        Level level = LogManager.getLogger(JobsService.class).getLevel();
        return level != null ? level.toString() : "INFO";
    }
}
