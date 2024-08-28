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
import sys
import zipfile
import dbFunctions as db

import helperFunctions as helper_functions
from loggingConfig import get_logger
logger = get_logger(__name__)


def decompress(name, id, conn):
    try:
        os.chdir("/tmp")
        os.mkdir("/tmp/" + id)
        logger.info("decompress() file name: %s", name)
        with zipfile.ZipFile("/tmp/" + name, "r") as zip_ref:
            zip_ref.extractall("/tmp/" + id)
            status = 30
            logger.debug("decompress() CSAR has been decompressed")
            db.update_app_status(status, id, conn)

    except Exception as error:
        decompress_error = "Decompressing error: %s", str(error)
        logger.error("decompress() error: %s",  str(error))
        status = 10
        db.app_error(status, decompress_error, id, conn)


class Main:
    if __name__ == '__main__':
        try:
            name = sys.argv[1]
            id = sys.argv[2]
            decompress(name, id, db.get_conn())
        finally:
            helper_functions.stop_istio_proxy()
