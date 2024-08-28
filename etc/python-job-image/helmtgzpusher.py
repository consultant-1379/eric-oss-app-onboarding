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
import re
import sys
import dbFunctions as db
import requests
import helperFunctions as helper_functions
from loggingConfig import get_logger

logger = get_logger(__name__)


def helm_reg(artifact_id, app_name, app_version, path_to_tgz, reg_url, auth, conn):
    os.chdir("/tmp")

    data = open(path_to_tgz, 'rb').read()
    file_path, file_name = os.path.split(path_to_tgz)
    rx = re.compile(r'(?P<name>.*)-(?P<version>\d+\.\d+\.\d[^.]*)\.(?P<crate>.*)')
    m = rx.search(file_name)
    if m:
        artifact_name = m.group("name")
        artifact_version = m.group("version")
    url = reg_url + "/api/" + app_name + "_" + app_version + "/charts"
    response = requests.post(url, data=data, auth=(auth['reg_user'], auth['reg_password']))
    logger.info("helm_reg() response: %s", str(response))

    if str(response) == '<Response [201]>':
        logger.debug("helm_reg() Helm Chart has been pushed successfully ")
        artifact_status = 30
        location = "/api/" + app_name + "_" + app_version + "/charts/" + artifact_name + "/" + artifact_version
        db.update_artifact_status(artifact_status, location, artifact_id, conn)
        logger.debug("helm_reg() Helm Chart name: %s , Helm Chart version: %s and artifact status: %s",
                     artifact_name, app_version, str(artifact_status))
    else:
        logger.error("helm_reg() Helm Chart has failed with status of %s", str(response))
        artifact_status = 10
        db.update_artifact_status(artifact_status, "", artifact_id, conn)
    return response


class Main:
    if __name__ == '__main__':
        try:
            reg_url = os.environ['HELM_REG_URL']
            auth = {'reg_user': os.environ['HELM_REG_USER'], 'reg_password': os.environ['HELM_REG_PASSWORD']}

            artifact_id = sys.argv[2]
            app_name = sys.argv[3]
            app_version = sys.argv[4]
            path_to_tgz = sys.argv[1]
            helm_reg(artifact_id, app_name, app_version, path_to_tgz, reg_url, auth, db.get_conn())
        finally:
            helper_functions.stop_istio_proxy()