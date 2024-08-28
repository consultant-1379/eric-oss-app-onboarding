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

import glob
import os
import re
import sys

import psycopg2
import requests
import semantic_version
import yaml

import dbFunctions as db
import helperFunctions as helper_functions
from constants import LCM_GET_APPS_ENDPOINT
from loggingConfig import get_logger

logger = get_logger(__name__)

toscafile = ""


def parse(app_id, conn):
    global toscafile
    try:
        os.chdir("/tmp")
        pr = os.listdir(app_id)
        for root in pr:
            result = re.match("^[A-Za-z]etadata$", root)
            if result:
                file_result = result.group()
                toscafile = open(app_id + "/" + file_result + "/Tosca.meta", "r")  # TOSCA
                break
        tosca_yaml_file = yaml.safe_load(toscafile)

        app_desc_path = tosca_yaml_file['Entry-Definitions']
        vendor = tosca_yaml_file["Created-By"]
        app_descriptor = open(r"" + app_id + "/" + app_desc_path)
        app_descriptor_yaml_file = yaml.safe_load(app_descriptor)  # APP descriptor
        app_name = app_descriptor_yaml_file["Description of an APP"]['APPName']
        app_version = str(app_descriptor_yaml_file["Description of an APP"]['APPVersion'])
        app_type = app_descriptor_yaml_file["Description of an APP"]['APPType']

        kafka_bdr_permissions = permissions_parser(app_descriptor_yaml_file)
        roles_tuple_list = roles_parser(app_descriptor_yaml_file, app_id)

        asd_path = app_descriptor_yaml_file['APPComponent']['Path']  # ASD
        descriptor_list = [app_desc_path, app_descriptor, app_name, app_version, asd_path, vendor, app_type]
        descriptor_list_titles = ["app_desc_path", "app_descriptor", "app_name", "app_version", "asd_path", "vendor",
                                  "app_type"]
        check_null(descriptor_list, descriptor_list_titles)
        check_app_versions(app_name, app_version, conn)

        logger.info("parse() parsing completed - getting artifacts")
        for file in glob.glob(app_id + "/[A-Za-z]ther[A-Za-z]efinitions/[A-Za-z]SD/*.tgz"):

            file_path, file_name = os.path.split(file)
            rx = re.compile(r'(?P<name>.*)-(?P<version>\d+\.\d+\.\d[^.]*)\.(?P<crate>.*)')
            m = rx.search(file_name)
            if m:
                artefact_name = m.group("name")
                artefact_version = m.group("version")
                db.onboard_artifacts(artefact_name, artefact_version, "H", file, 20, app_id, conn)

        for file in glob.glob(app_id + "/[A-Za-z]ther[A-Za-z]efinitions/[A-Za-z]SD/[A-Za-z]mages/*.tar"):
            file_path, file_name = os.path.split(str(file))
            artefact_name = file_name
            db.onboard_artifacts(artefact_name, "--", "I", file, 20, app_id, conn)

        save_permissions(kafka_bdr_permissions, app_id, conn)

        if len(roles_tuple_list) > 0:
            db.onboard_roles(roles_tuple_list, conn)
        else:
            logger.debug("parse() no roles found in App descriptor, no role will be added")

        db.update_app_details(app_name, app_version, vendor, app_type, 40, app_id, conn)
        return "PARSING COMPLETE"
    except (Exception, psycopg2.Error, ValueError) as error:
        parse_error = "Parsing error: " + str(error)
        logger.error("parse() parsing error: %s", str(error))
        db.app_error(10, parse_error, app_id, conn)


def permissions_parser(app_descriptor_yaml_file):
    try:
        kafka_bdr_permissions = [("kafka", "GLOBAL")]
        if 'APPPermissions' in app_descriptor_yaml_file and app_descriptor_yaml_file['APPPermissions'] is not None:
            list_of_permissions = app_descriptor_yaml_file['APPPermissions']

            for i in list_of_permissions:
                i = {k.upper(): v for k, v in i.items()}
                if i.get("RESOURCE").upper() == "BDR":
                    resource_name = i.get("RESOURCE").lower()
                    scope = i.get("SCOPE")
                    kafka_bdr_permissions.append((resource_name, scope))
    except:
        kafka_bdr_permissions = None

    return kafka_bdr_permissions


def save_permissions(kafka_bdr_permissions, app_id, conn):
    if kafka_bdr_permissions:
        permissions_to_store = []
        for resource_name, scope in kafka_bdr_permissions:
            if resource_name.upper() == "BDR" and not scope:
                empty_scope_error = 'Scope must be given for the resource: ' + resource_name
                logger.error("save_permissions() %s", empty_scope_error)
                raise Exception(empty_scope_error)

            permissions_to_store.append((resource_name, scope, app_id))

        db.onboard_permissions_batch(permissions_to_store, conn)


def validate_version(app_version, current_version):
    validate = semantic_version.validate(app_version)
    if validate:
        if not current_version:
            logger.debug("validate_version() current version does not exist")
        else:
            check = semantic_version.compare(app_version, current_version)
            if check == 1:
                logger.debug(
                    "validate_version() valid version, continue with parsing - current version is %s version uploaded is %s"
                    , current_version, app_version)
            else:
                logger.error(
                    "validate_version() greater or same version already exists, current version is: %s version uploaded is %s"
                    , current_version, app_version)
                raise ValueError(' Greater or same version already exists ')

    else:
        logger.error("validate_version() invalid version: %s", app_version)
        raise ValueError(' Invalid Version ')


def check_v3_lcm_app_name_and_version(app_name, app_version):
    try:
        lcm_host = os.environ['APP_LCM_SERVICE_HOSTNAME']
        lcm_port = os.environ['APP_LCM_SERVICE_PORT']
        lcm_get_app_url = "http://" + lcm_host + ":" + lcm_port + "/" + LCM_GET_APPS_ENDPOINT + "?name=" + app_name
        response = requests.get(lcm_get_app_url)

        if response.status_code == 200:
            apps_list = response.json()

            if len(apps_list["items"]) > 0:
                try:
                    for app in apps_list["items"]:
                        lcm_app_version = app["version"]
                        validate_version(app_version, lcm_app_version)
                except ValueError:
                    logger.error(
                        "check_v3_lcm_app_name_and_version() Greater or same version already exists in App-LCM, "
                        "App name: %s, version: %s", app_name, app_version)
                    raise ValueError(' Greater or same version already exists in APP-LCM ')
        else:
            error_resp = 'Error accessing LCM: response code: ' + str(response.status_code) + \
                         ', message: ' + response.text
            logger.error("check_v3_lcm_app_name_and_version() %s", error_resp)
            raise ValueError(error_resp)
    except Exception as e:
        logger.error("check_v3_lcm_app_name_and_version() error: %s", e)
        raise ValueError(str(e))


def check_null(parse_list, descriptor_list_titles):
    for index, x in enumerate(parse_list):
        if not x:
            logger.error("check_null() value %s is null", descriptor_list_titles[index])
            raise ValueError('Null value found!')
        else:
            continue


def check_app_versions(app_name, app_version, conn):
    logger.info("check_app_versions() App name: %s and App version: %s", app_name, app_version)
    try:
        cursor = conn.cursor()
        postgres_update_query = "SELECT version from Application WHERE name =(%s)"
        cursor.execute(postgres_update_query, [app_name])
        records = cursor.fetchall()

        if not records:
            validate_version(app_version, "")
        for row in records:
            current_version = row[0]
            validate_version(app_version, current_version)
        conn.commit()

        check_v3_lcm_app_name_and_version(app_name, app_version)

        logger.debug("check_app_versions() retrieved latest App version")
        return "Retrieved latest app version"
    except (Exception, psycopg2.Error, ValueError) as error:
        logger.error("check_app_versions() error: %s", str(error))
        raise ValueError("Error" + str(error))


def roles_parser(app_descriptor_yaml_file, app_id):
    try:
        roles_tuple_list = [(role['Name'], app_id) for role in app_descriptor_yaml_file['APPRoles']]
    except:
        roles_tuple_list = []
    return roles_tuple_list


class Main:
    if __name__ == '__main__':
        try:
            app_id = sys.argv[1]
            parse(app_id, db.get_conn())
        finally:
            helper_functions.stop_istio_proxy()
