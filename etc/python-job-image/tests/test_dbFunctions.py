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
from unittest.mock import patch, MagicMock

import psycopg2

from dbFunctions import onboard_artifacts, update_app_status, update_artifact_status, app_error, update_app_details, \
    get_app_status, get_artifact_status, update_artifact, update_multiple_artifacts_status, onboard_roles, get_conn, \
    get_app_events, update_single_artifact_status, onboard_permissions_batch


class Test(unittest.TestCase):

    def test_onboard_artifacts(self):
        with patch('psycopg2.connect') as mock_connect:
            mock_connect = MagicMock()
            mock_cursor = MagicMock()
            mock_connect.cursor.return_value = mock_cursor
            result = onboard_artifacts("test", "test", "test", "test", "test", "test", mock_connect)
            print(result)

            self.assertIsNotNone(result)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_onboard_artifacts_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                onboard_artifacts("test", "test", "test", "test", "test", "test", mock_connect)

    def test_update_app_status(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Updated application status to: 50"
            mock_connect = MagicMock()  # Mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = update_app_status("50", "1", mock_connect)
            # Update app status to 50 (Onboarded)

            print(result)

            self.assertEqual(result, expected)  # Expected result should be "Updated application status to: 50"

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_update_app_status_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                update_app_status("50", "1", mock_connect)

    def test_update_artifact_status(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Updated Artifact status to: 30"
            mock_connect = MagicMock()  # Mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = update_artifact_status("30", "test", "1", mock_connect)
            # Update artifact status to 30 (Completed)
            print(result)

            self.assertEqual(result, expected)  # Expected result should be "Updated Artifact status to: 30"

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_update_artifact_status_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                update_artifact_status("30", "test", "1", mock_connect)

    def test_app_error(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Updated application status to: 10"
            mock_connect = MagicMock()  # Mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = app_error("10", "Error", "1", mock_connect)
            # Update artifact status to 10 (Failed)

            print(result)

            self.assertEqual(result, expected)  # Expected result should be "Updated application status to: 10"

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_app_error_raise_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                app_error("10", "Error", "1", mock_connect)

    def test_update_app_details(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Application details updated"
            mock_connect = MagicMock()  # Mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = update_app_details("test", "1.0.1", "test", "rApp", "20", "1", mock_connect)
            # Update Application details e.g name,version,type

            print(result)

            self.assertEqual(result, expected)  # Expected result should be "Application details updated"

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_update_app_details_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                update_app_details("test", "1.0.1", "test", "rApp", "20", "1", mock_connect)

    def test_get_app_status(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "50"
            db_query_return_value = (expected,)
            mock_connect = MagicMock()  # Mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchone.return_value = db_query_return_value
            mock_connect.cursor.return_value = mock_cursor
            result = get_app_status("2", mock_connect)

            print(result)
            self.assertEqual(expected, result)  # Expected result should be "50"

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_get_app_status_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                get_app_status("2", mock_connect)

    def test_get_artifact_status(self):
        with patch('psycopg2.connect') as mock_connect:
            expected_return_value = "30"
            db_query_return_value = (expected_return_value,)
            mock_connect = MagicMock()  # mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchone.return_value = db_query_return_value
            mock_connect.cursor.return_value = mock_cursor
            result = get_artifact_status("2", mock_connect)

            print("test_get_artifact_status Return value is " + str(result))
            self.assertEqual(expected_return_value, result)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_get_artifact_status_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                get_artifact_status("2", mock_connect)

    def test_update_artifact(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Updated Artifact details"
            mock_connect = MagicMock()
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = update_artifact("eric-oss-cnr5gassist", "1.0.0",
                                     "/v2/proj-eric-oss-drop/eric-oss-cnr5gassist/manifests/1.1.108-1", 1, mock_connect)

            print(result)

            self.assertEqual(result, expected)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_update_artifact_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                update_artifact("eric-oss-cnr5gassist", "1.0.0",
                                "/v2/proj-eric-oss-drop/eric-oss-cnr5gassist/manifests/1.1.108-1", 1, mock_connect)

    def test_update_multiple_artifacts_status(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Updated multiple artifacts with status to: 30"
            mock_connect = MagicMock()
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = update_multiple_artifacts_status(30, [1, 2, 3], mock_connect)
            print(result)

            self.assertEqual(result, expected)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_update_multiple_artifacts_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                update_multiple_artifacts_status(30, [1, 2, 3], mock_connect)

    def test_add_appRoles(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Roles added"
            mock_connect = MagicMock()
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = onboard_roles([("admin",1)], mock_connect)
            print(result)

            self.assertEqual(result, expected)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_add_appRoles_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                onboard_roles([("admin",1)], mock_connect)

    def test_get_conn(self):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            with patch('psycopg2.connect') as mock_connect:
                mock_connect.return_value = MagicMock()
                get_conn()

                mock_connect.assert_called_with(user='user',
                                                password='password',
                                                host='host',
                                                port='5432',
                                                database='onboarding')

    @patch('constants.DB_ERROR_RETRY_COUNT', 2)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 1)
    @patch('psycopg2.connect')
    def test_get_conn_failed_dberror(self, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")

            with self.assertRaises(RuntimeError):
                get_conn()

    def test_onboard_permissions_batch(self):
        with patch('psycopg2.connect') as mock_connect:
            expected = "Permissions added"
            mock_connect = MagicMock()
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = expected
            mock_connect.cursor.return_value = mock_cursor
            result = onboard_permissions_batch([("test-resource", "scope", 1)], mock_connect)

            self.assertIsNotNone(result)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_onboard_permissions_batch_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                onboard_permissions_batch([("test-resource", "scope", 1)], mock_connect)

    def test_get_app_events(self):
        with patch('psycopg2.connect') as mock_connect:
            expected_return_value = "App event"
            db_query_return_value = (expected_return_value,)
            mock_connect = MagicMock()  # mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = db_query_return_value
            mock_connect.cursor.return_value = mock_cursor
            result = get_app_events("2", mock_connect)

            self.assertEqual(db_query_return_value, result)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_get_app_events_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                get_app_events("2", mock_connect)

    def test_update_single_artifact_status(self):
        with patch('psycopg2.connect') as mock_connect:
            expected_return_value = "Updated Artifact details"
            mock_connect = MagicMock()  # mock db connection
            mock_cursor = MagicMock()
            mock_cursor.fetchone.return_value = expected_return_value
            mock_connect.cursor.return_value = mock_cursor
            result = update_single_artifact_status("30", "2", mock_connect)

            self.assertEqual(expected_return_value, result)

    @patch('constants.DB_ERROR_RETRY_COUNT', 1)
    @patch('constants.DB_ERROR_RETRY_INTERVAL', 0.001)
    @patch('psycopg2.connect')
    @patch('logging.error')
    def test_update_single_artifact_status_error(self, mock_logger, mock_connect):
        with patch.dict('os.environ',
                        {'DB_USER': 'user',
                         'DB_PASSWORD': 'password',
                         'DB_HOST': 'host',
                         'DB_PORT': '5432',
                         'DB_NAME': 'onboarding'}):
            mock_connect.side_effect = psycopg2.DatabaseError("Connection error")
            mock_connect.commit.side_effect = psycopg2.DatabaseError('Commit error')

            with self.assertRaises(psycopg2.DatabaseError):
                update_single_artifact_status("30", "2", mock_connect)
