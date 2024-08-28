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
import runpy
import sys

from unittest.mock import patch, MagicMock, ANY

import requests
from pyfakefs import fake_filesystem_unittest

import dbFunctions
import helperFunctions
from helmtgzpusher import helm_reg

path_to_csar = ""
mock_connect = MagicMock()


class Test(fake_filesystem_unittest.TestCase):
    def setUp(self):
        global path_to_csar
        path = os.path.abspath(__file__) # Get correct path to /tests
        file_path, file_name = os.path.split(path)
        path_to_csar = file_path
        self.setUpPyfakefs()  # set up fake filesystem
        if not self.fs.exists("/tmp"):
            self.fs.add_real_directory("/tmp", False, False, "/tmp")
        self.fs.add_real_directory(file_path + "/testFiles/DecompressedCSAR", False, False, "/tmp/1")

    @patch.object(requests, 'post')
    def test_helm_reg(self, mockpost):
        mockresponse = "<Response [201]>"
        mockpost.return_value = mockresponse

        response = helm_reg("1", "test", "1.0.0", "1/OtherDefinitions/ASD/eric-oss-app-onboarding-0.1.0-1.tgz", "test",
                            {"reg_user": "test", "reg_password": "test"}, mock_connect)

        self.assertEqual(response, mockresponse)  # Successful post, response 201, artifact status will change to 30

    @patch.object(requests, 'post')
    def test_helm_reg_conflict(self, mockpost):
        mockresponse = "<Response [409]>"
        mockpost.return_value = mockresponse

        response = helm_reg("1", "test", "1.0.0", "1/OtherDefinitions/ASD/eric-oss-app-onboarding-0.1.0-1.tgz", "test",
                            {"reg_user": "test", "reg_password": "test"}, mock_connect)

        self.assertEqual(response, mockresponse)  # Failed post, response 409, artifact status will change to 10

    @patch.object(requests, 'post')
    def test_helm_reg_bad_request(self, mockpost):
        mockresponse = "<Response [404]>"
        mockpost.return_value = mockresponse

        response = helm_reg("1", "test", "1.0.0", "1/OtherDefinitions/ASD/eric-oss-app-onboarding-0.1.0-1.tgz", "test",
                            {"reg_user": "test", "reg_password": "test"}, mock_connect)

        self.assertEqual(response, mockresponse)  # Failed post, response 404, artifact status will change to 10

    @patch.object(requests, 'post')
    def test_helm_reg_file_not_found(self, mockpost):
        mockresponse = "<Response [404]>"
        mockpost.return_value = mockresponse

        try:
            response = helm_reg("1", "test", "1.0.0", "1/OtherDefinitions/ASD/eric-oss-app-onboarding-0.1.0-2.tgz",
                                "test",  {"reg_user": "test", "reg_password": "test"}, mock_connect)
        except FileNotFoundError as error:
            pass  # File not found

    @patch('requests.post')
    @patch('dbFunctions.update_artifact_status')
    @patch.object(dbFunctions, 'get_conn')
    @patch('os.environ')
    @patch.object(helperFunctions, 'stop_istio_proxy')
    def test_main_success(self, mock_istio, mock_environ, mock_connect, mock_update_artifact_status, mock_post):
        mock_istio.return_value = None
        mock_environ.return_value = None
        mock_connect.return_value = MagicMock()
        mock_post.return_value = '<Response [201]>'
        path_to_tgz = "1/OtherDefinitions/ASD/eric-oss-app-onboarding-0.1.0-1.tgz"
        artifact_id = '1'
        app_name = 'test'
        app_version = '1.0.0'
        mock_argv = ['', path_to_tgz, artifact_id, app_name, app_version]

        with patch.object(sys, 'argv', mock_argv):
            runpy.run_module('helmtgzpusher', run_name='__main__')

        mock_post.assert_called()
        mock_update_artifact_status.assert_called_with(30, '/api/test_1.0.0/charts/eric-oss-app-onboarding/0.1.0-1', '1', ANY)
        mock_istio.assert_called()

    @patch('requests.post')
    @patch('dbFunctions.update_artifact_status')
    @patch.object(dbFunctions, 'get_conn')
    @patch('os.environ')
    @patch.object(helperFunctions, 'stop_istio_proxy')
    def test_main_error(self, mock_istio, mock_environ, mock_connect, mock_update_artifact_status, mock_post):
        mock_istio.return_value = None
        mock_environ.return_value = None
        mock_connect.return_value = MagicMock()
        mock_post.return_value = '<Response [404]>'
        path_to_tgz = "1/OtherDefinitions/ASD/eric-oss-app-onboarding-0.1.0-1.tgz"
        artifact_id = '1'
        app_name = 'test'
        app_version = '1.0.0'
        mock_argv = ['', path_to_tgz, artifact_id, app_name, app_version]

        with patch.object(sys, 'argv', mock_argv):
            runpy.run_module('helmtgzpusher', run_name='__main__')

        mock_post.assert_called()
        mock_update_artifact_status.assert_called_with(10, '', '1', ANY)
        mock_istio.assert_called()
