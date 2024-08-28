#
# COPYRIGHT Ericsson 2023
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

import importlib
import os
import runpy
import sys
import unittest
from unittest.mock import patch, MagicMock, ANY

import dbFunctions
import helperFunctions
import jobService

main_job = None


class Test(unittest.TestCase):

    def setUp(self):
        os.environ['NAMESPACE'] = 'test_ns'
        os.environ['TTL_SECONDS_AFTER_FINISHED'] = '1200'
        global main_job
        main_job = importlib.import_module("main-job")

    @patch('os.system')
    def test_clean(self, mock_system):
        mock_argv = ["1", "test-app", "1.0.0"]

        with patch.object(sys, 'argv', mock_argv):
            main_job.clean()

        self.assertTrue(('/tmp' == os.getcwd()) or ('C:\\tmp' == os.getcwd()))
        mock_system.assert_called()

    @patch.object(dbFunctions, 'get_conn')
    def test_job_trigger_clean(self, mock_connect):
        mock_connect = MagicMock()  # Mock the db connection
        mock_connect.close.return_value = None
        mock_clean = MagicMock()
        main_job.clean = mock_clean

        main_job.job_trigger(10, mock_connect)
        mock_clean.assert_called()
        mock_connect.close.assert_called()

    @patch.object(jobService, 'create_job_object')
    @patch.object(dbFunctions, 'get_conn')
    def test_job_trigger_decompress(self, mock_connect, mock_create_job_object):
        mock_connect = MagicMock()  # Mock the db connection
        mock_argv = ["1", "test-app", "1.0.0", "image.tar"]

        with patch.object(sys, 'argv', mock_argv):
            main_job.job_trigger(20, mock_connect)

        mock_create_job_object.assert_called()

    @patch.object(jobService, 'create_job_object')
    @patch.object(dbFunctions, 'get_conn')
    def test_job_trigger_parse(self, mock_connect, mock_create_job_object):
        mock_connect = MagicMock()  # Mock the db connection
        mock_argv = ["1", "test-app", "1.0.0", "image.tar"]

        with patch.object(sys, 'argv', mock_argv):
            main_job.job_trigger(30, mock_connect)

        mock_create_job_object.assert_called()

    @patch('time.sleep')
    @patch.object(dbFunctions, 'get_conn')
    def test_job_trigger_get_artifacts(self, mock_connect, mock_sleep):
        mock_connect = MagicMock()  # Mock the db connection
        mock_get_artifacts = MagicMock()
        main_job.get_artifacts = mock_get_artifacts

        main_job.job_trigger(40, mock_connect)
        mock_get_artifacts.assert_called()

    @patch.object(dbFunctions, 'get_conn')
    def test_job_trigger_clean_onboard(self, mock_connect):
        mock_connect = MagicMock()  # Mock the db connection
        mock_connect.close.return_value = None
        mock_clean = MagicMock()
        main_job.clean = mock_clean

        main_job.job_trigger(50, mock_connect)
        mock_clean.assert_called()
        mock_connect.close.assert_called()

    @patch.object(dbFunctions, 'get_conn')
    def test_job_trigger_clean_else(self, mock_connect):
        mock_connect = MagicMock()  # Mock the db connection
        mock_connect.close.return_value = None
        mock_clean = MagicMock()
        main_job.clean = mock_clean

        main_job.job_trigger(60, mock_connect)
        mock_clean.assert_called()
        mock_connect.close.assert_called()

    @patch.object(dbFunctions, 'get_conn')
    def test_check_app_status(self, mock_connect):
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [[20]]
        mock_connect.cursor.return_value = mock_cursor
        main_job.job_trigger.return_value = MagicMock()

        main_job.check_app_status(1, mock_connect)
        self.assertEqual(20, main_job.app_state)

    @patch.object(dbFunctions, 'get_conn')
    def test_check_app_status_app_not_found(self, mock_connect):
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = []
        mock_connect.cursor.return_value = mock_cursor
        main_job.job_trigger.return_value = MagicMock()

        main_job.check_app_status(1, mock_connect)
        self.assertEqual(-1, main_job.app_state)

    @patch('os.system')
    @patch.object(dbFunctions, 'get_conn')
    @patch.object(helperFunctions, 'stop_istio_proxy')
    def test_main_init(self, mock_istio, mock_connect, mock_os):
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [[20]]
        mock_connect.cursor.return_value = mock_cursor
        mock_argv = ["1", "test-app", "1.0.0", "image.tar"]

        with patch.object(sys, 'argv', mock_argv):
            runpy.run_module('main-job', run_name='__main__')

        self.assertTrue(('/tmp' == os.getcwd()) or ('C:\\tmp' == os.getcwd()))
        mock_os.assert_called()
        mock_istio.assert_called()

    @patch('time.sleep')
    @patch.object(dbFunctions, 'get_app_status')
    @patch.object(dbFunctions, 'get_conn')
    @patch('jobService.create_job_object')
    @patch('JobMonitor.monitor')
    def test_get_artifacts_helmtgzpusher(self, mock_monitor, mock_create_job_object, mock_connect, mock_get_app_status,
                                         mock_sleep):
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [[1, 'H', '/helm/location', 'test_app', '1.0.0']]
        mock_connect.cursor.return_value = mock_cursor
        mock_argv = ["1", "test-app", "1.0.0", "image.tar"]

        with patch.object(sys, 'argv', mock_argv):
            main_job.get_artifacts(mock_connect)

        mock_create_job_object.assert_called()
        mock_create_job_object.assert_called_with('python3 helmtgzpusher.py  /helm/location 1 test_app 1.0.0',
                                                  'push-helm', 'image.tar', 'test_ns', 1200, 'test-app', 1)
        mock_monitor.assert_called()

    @patch('time.sleep')
    @patch('JobMonitor.monitor')
    @patch('jobService.create_job_object')
    @patch.object(dbFunctions, 'get_conn')
    def test_get_artifacts_command_line(self, mock_connect, mock_create_job_object, mock_monitor, mock_sleep):
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [[1, 'I', '/helm/location', 'test_app', '1.0.0']]
        mock_connect.cursor.return_value = mock_cursor
        mock_argv = ["1", "test-app", "1.0.0", "image.tar"]

        with patch.object(sys, 'argv', mock_argv):
            main_job.get_artifacts(mock_connect)

        mock_create_job_object.assert_called()
        mock_create_job_object.assert_called_with('python3 command_line.py  /helm/location 1 test-app',
                                                  'push-image', 'image.tar', 'test_ns', 1200, 'test-app', 1)
        mock_monitor.assert_called()

    @patch('time.sleep')
    @patch('JobMonitor.monitor')
    @patch('jobService.create_job_object')
    @patch.object(dbFunctions, 'get_conn')
    def test_get_artifacts_command_line(self, mock_connect, mock_create_job_object, mock_monitor, mock_sleep):
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [[1, 'D', '/helm/location', 'test_app', '1.0.0']]
        mock_connect.cursor.return_value = mock_cursor
        mock_argv = ["1", "test-app", "1.0.0", "image.tar"]

        with patch.object(sys, 'argv', mock_argv):
            main_job.get_artifacts(mock_connect)

        assert not mock_create_job_object.called, 'method should not have been called'
        mock_monitor.assert_called()

    @patch('time.sleep')
    @patch.object(dbFunctions, 'app_error')
    @patch.object(dbFunctions, 'get_conn')
    def test_get_artifacts_failed_exception(self, mock_connect, mock_app_error, mock_sleep):
        mock_connect = MagicMock()  # Mock the db connection
        mock_cursor = MagicMock()
        mock_cursor.fetchall.side_effect = Exception('Commit error')
        mock_connect.cursor.return_value = mock_cursor
        mock_argv = ["1", "test-app", "1.0.0", "image.tar"]

        with patch.object(sys, 'argv', mock_argv):
            main_job.get_artifacts(mock_connect)

        # assert mock_app_error.called
        mock_app_error.assert_called_with(10, 'Commit error', 'test-app', ANY)
