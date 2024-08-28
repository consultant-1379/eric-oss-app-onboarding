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
import dbFunctions as db
import psycopg2
import constants

from loggingConfig import get_logger
logger = get_logger(__name__)

app_status = ""


def check_status(artifact_id, status, artifacts_size, artifacts_completed, app_id, conn):
    global app_status
    if status == constants.ARTIFACT_STATUS_FAILED:
        app_status = constants.APP_STATUS_FAILED
        db.update_app_status(app_status, app_id, conn)
    elif status == constants.ARTIFACT_STATUS_PENDING:
        app_status = constants.APP_STATUS_FAILED
        db.update_single_artifact_status(constants.ARTIFACT_STATUS_FAILED, artifact_id, db.get_conn())
        db.update_app_status(app_status, app_id, db.get_conn())
    elif status == constants.ARTIFACT_STATUS_COMPLETED:
        artifacts_completed.append(status)
        if len(artifacts_completed) == artifacts_size:
            app_status = constants.APP_STATUS_ONBOARDED
            db.update_app_status(app_status, app_id, conn)
    return app_status


def monitor(app_id, conn):
    try:
        cursor = conn.cursor()
        postgres_update_query = "SELECT artifact.id, artifact.status from artifact INNER JOIN application ON " \
                                "artifact.application_id=application.id WHERE application.id=(%s)"
        cursor.execute(postgres_update_query, [app_id])
        records = cursor.fetchall()
        artifacts_completed = []
        artifacts = cursor.rowcount
        for row in records:
            artifact_id = row[0]
            status = row[1]
            check_status(artifact_id, status, artifacts, artifacts_completed, app_id, conn)
            if status == constants.ARTIFACT_STATUS_FAILED:
                break
        logger.info("monitor() has completed a check")
        return "Monitor job has completed a check"

    except (Exception, psycopg2.Error) as error:
        logger.error("monitor() error: %s ", error)

    finally:
        # closing database connection if exists
        if conn:
            cursor.close()
            conn.close()
            logger.debug("monitor() PostgreSQL connection closed")

        time.sleep(5)


class Main:
    if __name__ == '__main__':
        app_id = sys.argv[1]
        while not (app_status == 10 or app_status == 50):
            monitor(app_id, db.get_conn())

        else:
            logger.info("main() monitor Job has finished")
