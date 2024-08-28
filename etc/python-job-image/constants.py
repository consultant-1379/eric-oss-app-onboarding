#
# COPYRIGHT Ericsson 2022
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

ENV_VAR_SERVICE_MESH_ENABLED = 'SERVICE_MESH_ENABLED'
SERVICE_MESH_ISTIO_KEY = "sidecar.istio.io/inject"
SERVICE_MESH_ISTIO_VALUE = "true"
LABEL_JOB_TAG_KEY = "jobTag"
LABEL_JOB_TAG_VALUE = "onboarding-jobs"
SERVICE_MESH_USER_VOLUME = "sidecar.istio.io/userVolume"
SERVICE_MESH_USER_VOLUME_MOUNT = "sidecar.istio.io/userVolumeMount"
SM_LCM_CONTAINER_REGISTRY_ISM_ACCESS = "eric-lcm-container-registry-ism-access"
SM_LCM_HELM_CHART_REGISTRY_ISM_ACCESS = "eric-lcm-helm-chart-registry-ism-access"
SM_APP_MGR_DATA_DOCUMENT_DB_ISM_ACCESS = "eric-appmgr-data-document-db"
MAX_BYTES_PYTHON_LOGS = 20000000
ARTIFACT_STATUS_COMPLETED = 30
ARTIFACT_STATUS_FAILED = 10
ARTIFACT_STATUS_PENDING = 20
APP_STATUS_FAILED = 10
APP_STATUS_ONBOARDED = 50
DB_ERROR_RETRY_COUNT = 5
DB_ERROR_RETRY_INTERVAL = 5
LCM_GET_APPS_ENDPOINT = "v3/apps"
