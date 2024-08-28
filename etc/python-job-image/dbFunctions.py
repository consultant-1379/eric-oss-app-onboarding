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
import time

import psycopg2

import constants
from loggingConfig import get_logger

logger = get_logger(__name__)


def get_conn():
    retry_count = 0
    user = os.environ['DB_USER']
    password = os.environ['DB_PASSWORD']
    host = os.environ['DB_HOST']
    port = os.environ['DB_PORT']
    database = os.environ['DB_NAME']

    while retry_count < constants.DB_ERROR_RETRY_COUNT:
        retry_count = retry_count + 1
        try:
            connection = psycopg2.connect(user=user,
                                          password=password,
                                          host=host,
                                          port=port,
                                          database=database)
            return connection
        except Exception as conn_error:
            sleep_time = constants.DB_ERROR_RETRY_INTERVAL * retry_count
            logger.error("get_conn() retry in %s seconds due to error connecting to the db: %s", sleep_time,
                         str(conn_error))
            time.sleep(sleep_time)

    raise RuntimeError("Unable to connect to the database.", host, port, database)


def retry():
    def decorator(func):
        def retry_function(*args, **kwargs):
            attempt = 0
            while attempt <= constants.DB_ERROR_RETRY_COUNT:
                try:
                    # Convert args (a tuple) to a list
                    new_args = list(args)

                    if attempt > 0:
                        # Modify the last element, conn of the list to a new connection
                        new_args[-1] = get_conn()
                        logger.info('retry() attempt %d: %s with a new connection instance', attempt + 1, func)
                    else:
                        logger.info('retry() attempt %d: %s with given connection instance', attempt + 1, func)

                    # Convert the modified list back to a tuple
                    new_args_tuple = tuple(new_args)

                    return func(*new_args_tuple, **kwargs)
                except (Exception, psycopg2.Error) as error:
                    attempt += 1
                    sleep_time = constants.DB_ERROR_RETRY_INTERVAL * attempt
                    logger.error('retry() Exception thrown when attempting to run %s, attempt %d of %d. Reason: %s',
                                 func, attempt, constants.DB_ERROR_RETRY_COUNT, str(error))
                    time.sleep(sleep_time)

            return func(*args, **kwargs)

        return retry_function

    return decorator


@retry()
def update_app_status(status, app_id, conn):
    try:
        cursor = conn.cursor()
        postgres_update_query = "UPDATE application SET status=(%s) WHERE id=(%s)"
        cursor.execute(postgres_update_query, [status, app_id])
        conn.commit()
        logger.info("update_app_status() updated App status to: %s", str(status))
        return "Updated application status to: " + str(status)
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("update_app_status() PostgreSQL connection closed")


@retry()
def update_artifact_status(artifact_status, location, artifact_id, conn):
    try:
        cursor = conn.cursor()
        if location:
            artifact_update_query = "UPDATE artifact SET status=(%s),location=(%s) WHERE id=(%s)"
            cursor.execute(artifact_update_query, [artifact_status, location, artifact_id])
        else:
            artifact_update_query = "UPDATE artifact SET status=(%s) WHERE id=(%s)"
            cursor.execute(artifact_update_query, [artifact_status, artifact_id])
        conn.commit()
        logger.info("update_artifact_status() updated artifact status to: %s", str(artifact_status))
        return "Updated Artifact status to: " + str(artifact_status)
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("update_artifact_status() PostgreSQL connection closed")


@retry()
def get_artifact_status(artifact_id, conn):
    try:
        cursor = conn.cursor()
        artifact_status_query = "SELECT status from artifact where id = (%s)"
        cursor.execute(artifact_status_query, [artifact_id])
        record = cursor.fetchone()
        artifact_status = record[0]
        conn.commit()
        logger.info("get_artifact_status() retrieved artifact status of: %s", artifact_id)
        return artifact_status
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("get_artifact_status() PostgreSQL connection closed")


@retry()
def app_error(status, text, app_id, conn):
    try:
        cursor = conn.cursor()
        postgres_update_query = "UPDATE application SET status=(%s) WHERE id=(%s)"
        cursor.execute(postgres_update_query, [status, app_id])
        postgres_insert_query = """INSERT INTO application_event (text,type,application_id) VALUES (%s,%s,%s) """
        cursor.execute(postgres_insert_query, [text, " ERROR ", app_id])
        conn.commit()
        logger.info("app_error() updated App status to: %s", str(status))
        return "Updated application status to: " + str(status)
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("app_error() PostgreSQL connection closed")


@retry()
def onboard_artifacts(artifact_name, artifact_version, artifact_type, artifact_location, artifact_status, app_id,
                      conn):
    cursor = conn.cursor()
    postgres_insert_query_new = """INSERT INTO artifact (name,version,type,
    location,status,application_id) VALUES (%s,%s,%s,%s,%s,%s) RETURNING id"""
    insert_new_artifact_record = (
        artifact_name, artifact_version, artifact_type, artifact_location, artifact_status, app_id)
    cursor.execute(postgres_insert_query_new, insert_new_artifact_record)
    id_of_new_artifact = cursor.fetchone()[0]
    conn.commit()
    logger.info("onboard_artifacts() artifact added")
    return id_of_new_artifact


@retry()
def onboard_permissions_batch(permissions_list, conn):
    cursor = conn.cursor()
    insert_permission_query = "INSERT INTO permission (resource, scope, application_id) VALUES (%s, %s, %s)"
    cursor.executemany(insert_permission_query, permissions_list)
    conn.commit()
    logger.info("onboard_permissions_batch() permissions added")
    return "Permissions added"


@retry()
def update_app_details(app_name, app_version, vendor, app_type, status, app_id, conn):
    try:
        cursor = conn.cursor()
        postgres_update_query = "UPDATE application SET name=(%s),version=(%s),vendor=(%s),type=(%s),status=(%s) " \
                                "WHERE id=(%s) "
        cursor.execute(postgres_update_query, [app_name, app_version, vendor, app_type, status, app_id])
        conn.commit()
        logger.info("update_app_details() App details added")
        return "Application details updated"
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("update_app_details() PostgreSQL connection closed")


@retry()
def get_app_status(app_id, conn):
    try:
        cursor = conn.cursor()
        postgres_update_query = "SELECT status from application where id = (%s)"
        cursor.execute(postgres_update_query, [app_id])
        record = cursor.fetchone()
        app_state = record[0]
        conn.commit()
        logger.info("get_app_status() retrieved App status of %s", app_id)
        return app_state
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("get_app_status() PostgreSQL connection closed")


@retry()
def get_app_events(app_id, conn):
    try:
        cursor = conn.cursor()
        postgres_update_query = "SELECT * from application_event WHERE application_id = (%s)"
        cursor.execute(postgres_update_query, [app_id])
        record = cursor.fetchall()
        conn.commit()
        logger.info("get_app_events() retrieved App events of %s", app_id)
        return record
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("get_app_events() PostgreSQL connection closed")


@retry()
def update_artifact(name, version, location, artifact_id, conn):
    try:
        cursor = conn.cursor()
        artifact_update_query = "UPDATE artifact SET name=(%s),version=(%s),location=(%s) WHERE id=(%s)"
        cursor.execute(artifact_update_query, [name, version, location, artifact_id])
        conn.commit()
        logger.info("update_artifact() updated artifact details")
        return "Updated Artifact details"
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("update_artifact() PostgreSQL connection closed")


@retry()
def update_single_artifact_status(status, artifact_id, conn):
    try:
        cursor = conn.cursor()
        artifact_update_query = "UPDATE artifact SET status=(%s) WHERE id=(%s)"
        cursor.execute(artifact_update_query, [status, artifact_id])
        conn.commit()
        logger.info("update_single_artifact_status() updated artifact details")
        return "Updated Artifact details"
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("update_single_artifact_status() PostgreSQL connection closed")


@retry()
def update_multiple_artifacts_status(artifact_status, artifact_ids, conn):
    try:
        cursor = conn.cursor()
        placeholder = ['%s']
        placeholders = ', '.join(placeholder * len(artifact_ids))
        artifact_update_query = f'UPDATE artifact SET status=(%s) WHERE id IN ({placeholders})'
        params = [artifact_status]
        params.extend(artifact_ids)
        cursor.execute(artifact_update_query, params)
        conn.commit()
        logger.info("update_multiple_artifacts_status() updated multiple artifacts with status to: %s",
                    str(artifact_status))
        return "Updated multiple artifacts with status to: " + str(artifact_status)
    finally:
        # closing database connection.
        if conn:
            cursor.close()
            conn.close()
            logger.debug("update_multiple_artifacts_status() PostgreSQL connection closed")


@retry()
def onboard_roles(roles_tuple, conn):
    cursor = conn.cursor()
    postgres_insert_query_new = """INSERT INTO role (name,
    application_id) VALUES (%s,%s) """
    cursor.executemany(postgres_insert_query_new, roles_tuple)
    conn.commit()
    logger.info("onboard_roles() roles added ")
    return "Roles added"
