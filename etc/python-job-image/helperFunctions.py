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

import json
import os
import requests
import constants
from loggingConfig import get_logger

logger = get_logger(__name__)


def stop_istio_proxy():
    # json.loads() is used to convert the string values (true/false) to boolean values
    service_mesh_enabled = json.loads(os.getenv(constants.ENV_VAR_SERVICE_MESH_ENABLED).lower())

    if service_mesh_enabled:
        logger.info("stop_istio_proxy() Service Mesh is enabled. Calling /quitquitquit API")
        url = 'http://localhost:15000/quitquitquit'
        response = requests.post(url)

        if response.status_code == 200:
            logger.debug("stop_istio_proxy() termination of Istio sidecar is successful, response status code: %s",
                         str(response.status_code))
        else:
            logger.debug("stop_istio_proxy() termination of Istio sidecar is not successful, response status code: %s",
                         str(response.status_code))
    else:
        logger.info("stop_istio_proxy() Service Mesh is not enabled, no need to call /quitquitquit API")

def get_environment_variable_value(env, default_value):
    return os.environ.get(env, default_value)
