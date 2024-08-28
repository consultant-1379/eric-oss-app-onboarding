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

import dockertarpusher
import sys
import os
import dbFunctions as db
import helperFunctions as helper_functions
from loggingConfig import get_logger

logger = get_logger(__name__)


def main():
    if len(sys.argv) < 2:
        logger.debug("main() arguments required:  {Artifact_location , Artifact_id}")
        sys.exit(1)
    url = os.environ['CONTAINER_REGISTRY_HOST']
    tar_path = sys.argv[1]
    auth = {'login': os.environ['CONTAINER_REGISTRY_USER'], 'password': os.environ['CONTAINER_REGISTRY_PASSWORD'], 'verify': True}
    if "--noSslVerify" in sys.argv:
        auth['verify'] = False

    reg_client = dockertarpusher.Registry(url, tar_path, app_id=sys.argv[3], artifact_id=sys.argv[2],
                                          conn=db.get_conn(), stream=True, auth = auth)
    reg_client.get_images_from_tar()


class Main:
    if __name__ == '__main__':
        try:
            main()
        finally:
            helper_functions.stop_istio_proxy()
