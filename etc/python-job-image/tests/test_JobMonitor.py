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

import unittest
from unittest import mock
from unittest.mock import patch, MagicMock

from JobMonitor import monitor, check_status


class Test(unittest.TestCase):

    def test_check_status_artifact_completed(self):
        mock_connect = MagicMock()
        artifacts_size = 1  # passing in one artifact
        artifacts_completed = []
        result = check_status(1, 30, artifacts_size, artifacts_completed, "1", mock_connect)

        self.assertEqual(result, 50)  # As the only artifact is completed app status will be changed to 50

    def test_check_status_artifact_failed(self):
        mock_connect = MagicMock()
        artifacts_size = 1
        artifacts_completed = []
        result = check_status(1, 10, artifacts_size, artifacts_completed, "1", mock_connect)

        self.assertEqual(result, 10)  # As the only artifact is failed app status will be changed to 10

    def test_check_status_artifact_pending(self):
        with mock.patch("dbFunctions.get_conn") as mock_get_connection:
            mock_connect = MagicMock()
            artifacts_size = 1
            artifacts_completed = []
            result = check_status(1, 20, artifacts_size, artifacts_completed, "1", mock_connect)

            self.assertEqual(result, 10)  # As the only artifact is pending app status will be changed to 10

    @patch("time.sleep")
    def test_monitor(self, mock_time):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Monitor job has completed a check"
            mock_connect = MagicMock()  # Mock the db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = [(1, "10")]
            mock_connect.cursor.return_value = mock_cursor
            result = monitor("1", mock_connect)  # expected response should be "Monitor job has completed a check"
            print(result)

            self.assertEqual(result, expected)

    @patch("time.sleep")
    @patch("JobMonitor.check_status")
    def test_monitor_fail_more_than_one_artifact(self, mock_check_status, mock_time):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Monitor job has completed a check"
            mock_connect = MagicMock()  # Mock the db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = [(1, 10), (2, 10)]
            mock_connect.cursor.return_value = mock_cursor
            result = monitor("1", mock_connect)

            mock_check_status.assert_called_once()
            self.assertEqual(result, expected)

    @patch("time.sleep")
    @patch('logging.error')
    def test_monitor_error(self, mock_logger, mock_time):
        with patch('psycopg2.connect') as mock_connect:
            mock_connect = MagicMock()  # Mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.side_effect = Exception('Dummy error')
            mock_connect.cursor.return_value = mock_cursor

            monitor("1", mock_connect)

            self.assertRaises(Exception)
