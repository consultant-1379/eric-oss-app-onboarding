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
import time
import psycopg2
import jobService
from switch import Switch
from loggingConfig import get_logger
import dbFunctions as db
import JobMonitor as monitor
import helperFunctions as helper_functions


logger = get_logger(__name__)

app_state = ""
tmp_app = 0
namespace = os.environ['NAMESPACE']
ttl_after_seconds = int(os.getenv('TTL_SECONDS_AFTER_FINISHED'))


def job_trigger(app_status, conn):
    global tmp_app
    with Switch(app_status) as case:
        if case(10):
            conn.close()
            logger.debug("job_trigger() App has failed to onboard")
            clean()
        elif case(20):
            logger.debug("job_trigger() decompressor Job is being created")
            jobService.create_job_object("python3 decompressor.py '" + sys.argv[2] + "' " + sys.argv[1],
                                         "decompressor",
                                         sys.argv[3], namespace, ttl_after_seconds, sys.argv[1])
        elif case(30):
            logger.debug("job_trigger() parser Job is being created")
            jobService.create_job_object("python3 parser.py " + sys.argv[1],
                                         "parser", sys.argv[3], namespace, ttl_after_seconds, sys.argv[1])
        elif case(40):
            logger.debug("job_trigger() getting artifacts from db")
            if not tmp_app:
                tmp_app = 1
                get_artifacts(conn)
        elif case(50):
            conn.close()
            logger.debug("job_trigger() App is onboarded")
            clean()
        else:
            conn.close()
            logger.debug("job_trigger() App not found in db")
            clean()


def check_app_status(app_id, conn):
    global app_state
    try:
        cursor = conn.cursor()
        postgres_update_query = "SELECT status from application where id = (%s)"
        cursor.execute(postgres_update_query, [app_id])
        records = cursor.fetchall()
        for row in records:
            app_state = row[0]

        if len(records) == 0:
            app_state = -1

        job_trigger(app_state, conn)
    except (Exception, psycopg2.Error) as error:
        logger.error("check_app_status() failed with error: %s", str(error))


def clean():
    os.chdir("/tmp")
    os.system("rm -rf " + sys.argv[1])
    os.system("rm -rf '" + sys.argv[2] + "'")


def get_artifacts(conn):
    try:
        cursor = conn.cursor()
        postgres_get_artifacts_query = "SELECT artifact.id, artifact.type, artifact.location, application.name, " \
                                       "application.version from artifact INNER JOIN application ON " \
                                       "artifact.application_id=application.id WHERE application.id=(%s) "
        cursor.execute(postgres_get_artifacts_query, [sys.argv[1]])
        records = cursor.fetchall()
        for row in records:
            artifact_id = row[0]
            artifact_type = row[1]
            artifact_location = row[2]
            app_name = row[3]
            app_version = row[4]

            if artifact_type == "H":
                jobService.create_job_object(
                    "python3 helmtgzpusher.py " + " " + artifact_location + " " + str(
                        artifact_id) + " " + app_name + " "
                    + app_version, "push-helm", sys.argv[3], namespace, ttl_after_seconds, sys.argv[1], artifact_id)

            elif artifact_type == "I":
                jobService.create_job_object(
                    "python3 command_line.py " + " " + artifact_location + " " + str(artifact_id) + " " + sys.argv[1],
                    "push-image", sys.argv[3], namespace, ttl_after_seconds, sys.argv[1], artifact_id)

        monitor.monitor(sys.argv[1], db.get_conn())

    except (Exception, psycopg2.Error) as error:
        logger.error("get_artifacts() failed: %s", str(error))
        status = 10
        db.app_error(status, str(error), sys.argv[1], conn)

    finally:
        time.sleep(5)


class Main:
    if __name__ == '__main__':
        try:
            logger.info("main() Job is running for App ID: %s", sys.argv[1])
            while not (app_state == 10 or app_state == 50 or app_state == -1):
                logger.debug("main() current App status is %s", str(app_state))
                check_app_status(sys.argv[1], db.get_conn())
            else:
                logger.info("main() process finished with App status of %s", str(app_state))
        finally:
            helper_functions.stop_istio_proxy()
