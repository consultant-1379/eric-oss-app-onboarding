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
import shutil
from unittest.mock import MagicMock, patch

import requests
import yaml
from pyfakefs import fake_filesystem_unittest

import dbFunctions
import parser
from parser import parse, check_null, validate_version, permissions_parser, roles_parser

path_to_csar = ""
mock_connect = MagicMock()


class Test(fake_filesystem_unittest.TestCase):

    def setUp(self):
        os.environ['APP_LCM_SERVICE_HOSTNAME'] = 'localhost'
        os.environ['APP_LCM_SERVICE_PORT'] = '8080'
        os.environ['APP_MANAGER_APP_LCM_ROUTE_PATH'] = 'app-lifecycle-management'
        global path_to_csar
        path = os.path.abspath(__file__)  # Get correct path to /tests
        file_path, file_name = os.path.split(path)
        path_to_csar = file_path
        self.setUpPyfakefs()  # set up fake filesystem
        self.fs.add_real_directory(file_path + "/testFiles/DecompressedCSAR")  # add csar to filesystem
        if not self.fs.exists("/tmp"):
            self.fs.add_real_directory("/tmp", False, False, "/tmp")
        self.fs.add_real_directory(file_path + "/testFiles/test")

    @patch.object(requests, 'get')
    def test_parse(self, mock_get):
        mock_get.return_value = self.__get_v3_empty_app_details_mock_response(mock_get)
        shutil.move(path_to_csar + "/testFiles/DecompressedCSAR", "/tmp/1")  # move csar to /tmp/1
        self.assertEqual(parse("1", mock_connect), "PARSING COMPLETE")

    def test_parse_fail(self):
        shutil.move(path_to_csar + "/testFiles/DecompressedCSAR", "/tmp/1")  # move csar to /tmp/1
        try:
            parse("2", mock_connect)  # fails due to file not found
        except Exception:
            pass

    @patch.object(requests, 'get')
    def test_parse_success_lower_version_app_exists(self, mock_get):
        mock_get.return_value = self.__get_v3_apps_low_version_mock_response(mock_get)
        shutil.move(path_to_csar + "/testFiles/DecompressedCSAR", "/tmp/1")  # move csar to /tmp/1
        self.assertEqual(parse("1", mock_connect), "PARSING COMPLETE")

    @patch.object(requests, 'get')
    def test_parse_fail_app_exists_in_v3(self, mock_get):
        mock_get.return_value = self.__get_v3_app_details_mock_response(mock_get)
        shutil.move(path_to_csar + "/testFiles/DecompressedCSAR", "/tmp/1")  # move csar to /tmp/1
        self.assertNotEqual(parse("1", mock_connect), "PARSING COMPLETE")

    @patch.object(requests, 'get')
    def test_check_v3_lcm_app_version_fail_app_exists(self, mock_get):
        mock_get.return_value = self.__get_v3_app_details_mock_response(mock_get)
        with self.assertRaises(Exception):
            parser.check_v3_lcm_app_name_and_version("helloWorld", "1.0.0")

    @patch.object(requests, 'get')
    def test_check_v3_lcm_app_version_fail_lcm_conn_error(self, mock_get):
        mockresponse = mock_get
        mock_get.status_code = 404
        mock_get.return_value = mockresponse
        with self.assertRaises(Exception):
            parser.check_v3_lcm_app_name_and_version("helloWorld", "1.0.0")

    def test_check_null(self):
        try:
            check_null(["test", "test", "test"], ["desc1", "desc2", "desc3", "desc4"])  # No NULL value present, Should pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            pass

    def test_check_null_fail(self):
        try:
            check_null(["test", "", "test"], ["desc1", "desc2", "", "desc4"])  # NULL value present
        except ValueError:
            pass

    def test_validate_version(self):
        try:
            validate_version("1.0.2", "1.0.1")  # Valid version
        except Exception:
            self.fail('unexpected exception raised')
        else:
            pass

    def test_validate_version_invalid_version(self):
        try:
            validate_version("1.0", "1.0.1")  # 1.0 will fail as it's an invalid version
        except ValueError:
            pass

    def test_validate_version_higher_version_present(self):
        try:
            validate_version("1.0.0", "2.0.1")  # Will fail as 2.0.1 is a higher version than 1.0.0
        except ValueError:
            pass

    def test_permissions_parser(self):
        app_desc_path = open(path_to_csar + "/testFiles/DecompressedCSAR/Definitions/AppDescriptor.yaml")
        app_descriptor_yaml_file = yaml.safe_load(app_desc_path)
        permissions = permissions_parser(app_descriptor_yaml_file)
        self.assertTrue(("kafka", "GLOBAL") in permissions)
        self.assertTrue(("bdr", "readPolicy") in permissions)
        self.assertFalse(("Test_Resource", "dummy") in permissions)

    def test_permissions_parser_with_exception(self):
        app_desc_path = open(path_to_csar + "/testFiles/test")
        app_descriptor_yaml_file = yaml.safe_load(app_desc_path)

        permissions = permissions_parser(app_descriptor_yaml_file)
        self.assertIsNone(permissions)

    def test_permissions_parser_with_no_kafka_resource(self):
        app_desc_path = open(path_to_csar + "/testFiles/DecompressedCSAR/Definitions/AppDescriptor.yaml")
        app_descriptor_yaml_file = yaml.safe_load(app_desc_path)
        app_descriptor_yaml_file['APPPermissions'] = [permission for permission in
                                                      app_descriptor_yaml_file['APPPermissions'] if
                                                      permission['Resource'] != 'kafka']

        permissions = permissions_parser(app_descriptor_yaml_file)
        self.assertTrue(("kafka", "GLOBAL") in permissions)
        self.assertTrue(("bdr", "readPolicy") in permissions)
        self.assertFalse(("Test_Resource", "dummy") in permissions)

    def test_permissions_parser_with_no_permission(self):
        app_desc_path = open(path_to_csar + "/testFiles/DecompressedCSAR/Definitions/AppDescriptor.yaml")
        app_descriptor_yaml_file = yaml.safe_load(app_desc_path)
        app_descriptor_yaml_file['APPPermissions'] = None

        permissions = permissions_parser(app_descriptor_yaml_file)
        self.assertEqual(1, len(permissions))
        self.assertTrue(("kafka", "GLOBAL") in permissions)
        self.assertFalse(("bdr", "readPolicy") in permissions)

    def test_save_permissions(self):
        mock_conn = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value = mock_cursor
        permissions = [("kafka", "test"), ("bdr", "readPolicy")]
        parser.save_permissions(permissions, 1, mock_conn)

    def test_save_permissions_empty_kafka_scope(self):
        mock_conn = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value = mock_cursor
        permissions = [("kafka", ""), ("bdr", "readPolicy")]
        parser.save_permissions(permissions, 1, mock_conn)

    def test_save_permissions_error_empty_bdr_scope(self):
        mock_conn = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value = mock_cursor
        permissions = [("kafka", "test"), ("bdr", "")]

        with self.assertRaises(Exception):
            parser.save_permissions(permissions, 1, mock_conn)

    def test_roles_parser(self):
        try:
            app_desc_path=open(path_to_csar + "/testFiles/DecompressedCSAR/Definitions/AppDescriptor.yaml")
            app_descriptor_yaml_file = yaml.safe_load(app_desc_path)
            roles_tuple = roles_parser(app_descriptor_yaml_file,1)
            self.assertEqual(len(roles_tuple),1)
            self.assertEqual(roles_tuple[0][0],"admin")
        except Exception:
            self.fail('unexpected exception raised')
        else:
            pass

    def test_roles_parser_error(self):
        roles_tuple = roles_parser(None, 1)

        self.assertRaises(Exception)

    @patch.object(requests, 'get')
    @patch.object(dbFunctions, 'get_conn')
    def test_check_app_versions_success(self, mock_connect, mock_get):
        mock_get.return_value = self.__get_v3_empty_app_details_mock_response(mock_get)
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [['1.0.0']]
        mock_connect.cursor.return_value = mock_cursor
        res_expected = "Retrieved latest app version"

        res = parser.check_app_versions('test', '1.0.1', mock_connect)
        self.assertEqual(res_expected, res)

    @patch.object(requests, 'get')
    @patch.object(dbFunctions, 'get_conn')
    def test_check_app_versions_error(self, mock_connect, mock_get):
        mock_get.return_value = self.__get_v3_empty_app_details_mock_response(mock_get)
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [['1.0.0']]
        mock_connect.cursor.return_value = mock_cursor

        parser.check_app_versions('test', '1.0.1', mock_connect)
        self.assertRaises(ValueError)

    @patch.object(requests, 'get')
    @patch.object(dbFunctions, 'get_conn')
    def test_check_app_versions_unknown_error(self, mock_connect, mock_get):
        mock_get.return_value = self.__get_v3_empty_app_details_mock_response(mock_get)
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = []
        mock_connect.cursor.return_value = mock_cursor

        parser.check_app_versions('test', '1.0.1', mock_connect)
        self.assertRaises(Exception)

    @patch.object(requests, 'get')
    @patch.object(dbFunctions, 'get_conn')
    def test_check_app_versions_no_records(self, mock_connect, mock_get):
        mock_get.return_value = self.__get_v3_empty_app_details_mock_response(mock_get)
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = ''
        mock_connect.cursor.return_value = mock_cursor

        try:
            res = parser.check_app_versions('test', '1.0.1', mock_connect)
            self.assertEqual("Retrieved latest app version", res)
        except Exception:
            self.fail("parser.check_app_versions() raised unexpected Exception!")

    def __get_v3_empty_app_details_mock_response(self, mock_get):
        mockresponse = mock_get
        mock_get.status_code = 200
        mock_get.json.return_value = {"items": []}
        return mockresponse

    def __get_v3_app_details_mock_response(self, mock_get):
        mockresponse = mock_get
        mock_get.status_code = 200
        mock_get.json.return_value = {"items": [{
            "id": "db7ededd-463d-4be9-94c6-f02cfcdbcf9c",
            "type": "rApp",
            "name": "App-Onboarding-helloWorld",
            "version": "1.0.0",
            "mode": "DISABLED",
            "status": "CREATED"
        }, {
            "id": "db7ededd-463d-4be9-94c6-f02cfcdbcf9d",
            "type": "rApp",
            "name": "App-Onboarding-helloWorld",
            "version": "0.0.9",
            "mode": "DISABLED",
            "status": "CREATED"
        }]}
        return mockresponse

    def __get_v3_apps_low_version_mock_response(self, mock_get):
        mockresponse = mock_get
        mock_get.status_code = 200
        mock_get.json.return_value = {"items": [{
            "id": "db7ededd-463d-4be9-94c6-f02cfcdbcf9c",
            "type": "rApp",
            "name": "App-Onboarding-helloWorld",
            "version": "0.0.8",
            "mode": "DISABLED",
            "status": "CREATED"
        }, {
            "id": "db7ededd-463d-4be9-94c6-f02cfcdbcf9d",
            "type": "rApp",
            "name": "App-Onboarding-helloWorld",
            "version": "0.0.7",
            "mode": "DISABLED",
            "status": "CREATED"
        }]}
        return mockresponse
