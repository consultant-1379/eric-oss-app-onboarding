#
# COPYRIGHT Ericsson 2021
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#


import os
import string
import random
import time
import dbFunctions as db
import json
import constants
import helperFunctions

from loggingConfig import get_logger
from kubernetes import client, config, watch
from kubernetes.client import V1PodAffinityTerm, V1PodAffinity, V1LabelSelector, V1LabelSelectorRequirement, \
    ApiException
from kubernetes.config import load_incluster_config

jobNames = []
logger = get_logger(__name__)


# Mapping of operations to their associated status
OPERATION_STATUS_MAP = {
    "decompressor": [30, 10],
    "parser": [40, 10],
    "push-helm": [30, 10],
    "push-image": [30, 10],
}


def create_job_object(args, operation, image, namespace, ttl_after_seconds, app_id, artifact_id=""):
    S = 10  # number of characters in the string.
    ran = ''.join((random.choice(string.ascii_lowercase) for _ in range(S)))  # run loop until the define length

    logger.debug("create_job_object() args: %s", args)
    service_mesh_enabled = json.loads(os.getenv(constants.ENV_VAR_SERVICE_MESH_ENABLED).lower())
    tls_enabled = json.loads(os.getenv('TLS_ENABLED').lower())
    sm_user_volume = os.getenv('SM_USER_VOLUME')
    sm_user_volume_mount = os.getenv('SM_USER_VOLUME_MOUNT')
    service_id = os.getenv('SERVICE_ID')
    label = {}
    annotation = {}
    name = "job-" + operation + "-" + str(ran)
    container = client.V1Container(
        name=name,
        image=os.getenv('PYTHON_JOB_IMAGE_NAME'),
        command=["/bin/sh", "-c"],
        args=[args],
        env=env_vars(name, namespace, app_id, service_id),
        volume_mounts=[client.V1VolumeMount(name=os.getenv('STORAGE_VOLUME'), mount_path='/tmp')],
    )

    logs_streaming_method = helperFunctions.get_environment_variable_value("LOGS_STREAMING_METHOD", "indirect")

    if logs_streaming_method.lower() in ['direct', 'dual']:
        container.volume_mounts.append(client.V1VolumeMount(name=os.getenv('LOG_SHIPPER_VOLUME'), mount_path='/logs'))

    volume = client.V1Volume(
        name=os.getenv('STORAGE_VOLUME'),
        persistent_volume_claim=client.V1PersistentVolumeClaimVolumeSource(
            claim_name=os.getenv('PVC_NAME'))
    )

    affinity = client.V1Affinity(
        pod_affinity=V1PodAffinity(
            required_during_scheduling_ignored_during_execution=[
                V1PodAffinityTerm(topology_key="kubernetes.io/hostname",
                                  label_selector=V1LabelSelector(
                                      match_expressions=[
                                          V1LabelSelectorRequirement(
                                              key='statefulset.kubernetes.io/pod-name', operator='In',
                                              values=[os.getenv('INSTANCE_NAME')])]
                                  ))]))


    if service_mesh_enabled:
        # Adding extra Service Mesh related labels and annotation
        label = {constants.LABEL_JOB_TAG_KEY: constants.LABEL_JOB_TAG_VALUE,
                 constants.SERVICE_MESH_ISTIO_KEY: constants.SERVICE_MESH_ISTIO_VALUE,
                 "jobType": "onboarding-python-job"}
        annotation = {constants.SERVICE_MESH_ISTIO_KEY: constants.SERVICE_MESH_ISTIO_VALUE,
                      "proxy.istio.io/config": '{ "holdApplicationUntilProxyStarts": true }'}
        if tls_enabled:
            annotation[constants.SERVICE_MESH_USER_VOLUME] = sm_user_volume
            annotation[constants.SERVICE_MESH_USER_VOLUME_MOUNT] = sm_user_volume_mount

            label[constants.SM_LCM_CONTAINER_REGISTRY_ISM_ACCESS] = "true"
            label[constants.SM_LCM_HELM_CHART_REGISTRY_ISM_ACCESS] = "true"
            label[constants.SM_APP_MGR_DATA_DOCUMENT_DB_ISM_ACCESS] = "true"

    else:
        # Service Mesh is not enabled. Only default label is needed
        label = {constants.LABEL_JOB_TAG_KEY: constants.LABEL_JOB_TAG_VALUE}

    template = client.V1PodTemplateSpec(
        metadata=client.V1ObjectMeta(name=container.name, labels=label, annotations=annotation),
        spec=client.V1PodSpec(containers=[container], volumes=[volume],
                              image_pull_secrets=[client.V1LocalObjectReference(name='k8s-registry-secret')]
                              , restart_policy="Never",
                              service_account_name="eric-oss-app-onboarding-sa", affinity=affinity))

    if logs_streaming_method.lower() in ['direct', 'dual']:
        log_shipper_volume = client.V1Volume(
            name=os.getenv('LOG_SHIPPER_VOLUME'),
            persistent_volume_claim=client.V1PersistentVolumeClaimVolumeSource(
                claim_name=os.getenv('LOG_SHIPPER_PVC_NAME'))
        )
        template.spec.volumes.append(log_shipper_volume)

    spec = client.V1JobSpec(
        template=template,
        backoff_limit=5,
        ttl_seconds_after_finished=ttl_after_seconds
    )
    # Instantiate the deployment object
    job = client.V1Job(
        api_version="batch/v1",
        kind="Job",
        metadata=client.V1ObjectMeta(name=container.name,
                                     labels={constants.LABEL_JOB_TAG_KEY: constants.LABEL_JOB_TAG_VALUE}),
        spec=spec)
    jobNames.append(name)
    create_job(job, namespace)

    monitor_app_status_for_job_completion(operation=operation, job_name=name, namespace=namespace, app_id=app_id,
                                          artifact_id=artifact_id)


def create_job(job, namespace):
    load_incluster_config()
    api_instance = client.BatchV1Api()
    api_response = api_instance.create_namespaced_job(
        body=job,
        namespace=namespace)

    logger.debug("create_job() Job created, status: %s", str(api_response.status))


def monitor_app_status_for_job_completion(operation, job_name, namespace, app_id, artifact_id):
    complete = False
    error = ""
    logger.debug("monitor_app_status_for_job_completion() starting to check %s status", operation)

    status_values = OPERATION_STATUS_MAP.get(operation)
    if not status_values:
        error = f"UNKNOWN OPERATION: {operation}"
        logger.error("monitor_app_status_for_job_completion() error: %s", error)
    else:

        polling_time = int(os.getenv('PYTHON_JOB_POLLING_TIME'))
        max_poll = polling_time / 5
        number_of_polls = 0

        get_status_fn, id_param = get_status_check_fn_and_id(operation, app_id, artifact_id)

        while number_of_polls < max_poll:
            get_app_events(app_id)

            try:
                current_status = get_status_fn(id_param, db.get_conn())

                if current_status in status_values:
                    complete = operation_complete(operation, job_name, current_status, namespace)
                    break

                logger.debug("monitor_app_status_for_job_completion() %s is still ongoing - current status: %s",
                             operation, current_status)
                number_of_polls += 1

                error = check_polling_count(max_poll, number_of_polls, job_name, operation)
            except Exception as e:
                error = "monitor_app_status_for_job_completion() Exception: " + str(e)
                logger.error("monitor_app_status_for_job_completion() error: %s", e)

    if not complete:
        job_incomplete(job_name, namespace, error, app_id, db.get_conn())


def get_status_check_fn_and_id(operation, app_id, artifact_id):
    if operation in ["decompressor", "parser"]:
        return db.get_app_status, app_id
    else:
        return db.get_artifact_status, artifact_id


def check_polling_count(max_poll, number_of_polls, job_name, operation):
    error = None

    if number_of_polls >= max_poll:
        logger.debug("check_polling_count() number of retries has reached the maximum limit of %s", max_poll)
        error = f"Job {job_name} is not complete after {max_poll} retries"
    else:
        logger.debug("check_polling_count() going to check %s status again after 5 sec", operation)
        time.sleep(5)

    return error


def operation_complete(operation, job_name, current_status, namespace):
    if operation == "decompressor" or operation == "parser":
        logger.debug("operation_complete() %s is complete. Current App status: %s. Going to delete Job %s", operation,
                     current_status, job_name)
    else:
        logger.debug("operation_complete() %s is complete. Current artifact status: %s. Going to delete Job %s",
                     operation, current_status, job_name)
    delete_job(job_name, namespace)
    return True


def get_app_events(app_id):
    response = db.get_app_events(app_id, db.get_conn())
    for entry in response:
        logger.debug("get_app_events() events: %s", entry)


def job_incomplete(job_name, namespace, error_msg, app_id, conn):
    logger.debug("job_incomplete() going to delete ongoing Job: %s to stop it from retrying the usecase", job_name)
    # delete an ongoing or failed job
    delete_job(job_name, namespace)
    status = 10
    db.app_error(status, error_msg, app_id, conn)


def delete_jobs(namespace):
    for x in range(len(jobNames)):
        api_instance = client.BatchV1Api()
        api_instance.delete_namespaced_job(jobNames[x], namespace, body=client.V1DeleteOptions(
            propagation_policy='Foreground',
            grace_period_seconds=5))
        logger.debug("delete_jobs() Job with name %s has been deleted", jobNames[x])


def delete_job(job_name, namespace):
    api_instance = client.BatchV1Api()
    try:
        response = api_instance.delete_namespaced_job(job_name, namespace, body=client.V1DeleteOptions(
            propagation_policy='Foreground',
            grace_period_seconds=5))
        logger.debug("delete_job() response: %s", response)
        logger.debug("delete_job() Job with name %s has been deleted", job_name)
    except ApiException as e:
        logger.error("delete_job() Exception when calling BatchV1Api->delete_namespaced_job: %s", e)


def env_vars(name, namespace, app_id, service_id):
    env = [client.V1EnvVar(name="DB_USER", value=os.environ['DB_USER']),
           client.V1EnvVar(name="DB_PASSWORD", value=os.environ['DB_PASSWORD']),
           client.V1EnvVar(name="DB_HOST", value=os.environ['DB_HOST']),
           client.V1EnvVar(name="DB_PORT", value=os.environ['DB_PORT']),
           client.V1EnvVar(name="DB_NAME", value=os.environ['DB_NAME']),
           client.V1EnvVar(name="CONTAINER_REGISTRY_HOST", value=os.environ['CONTAINER_REGISTRY_HOST']),
           client.V1EnvVar(name="CONTAINER_REGISTRY_USER", value=os.environ['CONTAINER_REGISTRY_USER']),
           client.V1EnvVar(name="CONTAINER_REGISTRY_PASSWORD", value=os.environ['CONTAINER_REGISTRY_PASSWORD']),
           client.V1EnvVar(name="HELM_REG_URL", value=os.environ['HELM_REG_URL']),
           client.V1EnvVar(name="HELM_REG_USER", value=os.environ['HELM_REG_USER']),
           client.V1EnvVar(name="HELM_REG_PASSWORD", value=os.environ['HELM_REG_PASSWORD']),
           client.V1EnvVar(name="SERVICE_MESH_ENABLED", value=os.environ['SERVICE_MESH_ENABLED']),
           client.V1EnvVar(name="TLS_ENABLED", value=os.environ['TLS_ENABLED']),
           client.V1EnvVar(name="SM_USER_VOLUME", value=os.environ['SM_USER_VOLUME']),
           client.V1EnvVar(name="SM_USER_VOLUME_MOUNT", value=os.environ['SM_USER_VOLUME_MOUNT']),
           client.V1EnvVar(name="NAMESPACE", value=namespace),
           client.V1EnvVar(name="POD_NAME", value=name),
           client.V1EnvVar(name="CONTAINER_NAME", value=name),
           client.V1EnvVar(name="APP_ID", value=app_id),
           client.V1EnvVar(name="SERVICE_ID", value=service_id),
           client.V1EnvVar(name="LOGS_STREAMING_METHOD", value=os.environ['LOGS_STREAMING_METHOD']),
           client.V1EnvVar(name="LOGS_SEVERITY", value=os.environ['LOGS_SEVERITY']),
           client.V1EnvVar(name="APP_LCM_SERVICE_HOSTNAME", value=os.environ['APP_LCM_SERVICE_HOSTNAME']),
           client.V1EnvVar(name="APP_LCM_SERVICE_PORT", value=os.environ['APP_LCM_SERVICE_PORT'])
           ]
    return env


def main():
    load_incluster_config()
    client.BatchV1Api()


if __name__ == '__main__':
    main()
