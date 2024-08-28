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
import unittest
from unittest import mock
from unittest.mock import MagicMock, patch, ANY

import kubernetes

import dbFunctions
import jobService
import helperFunctions
from jobService import delete_job, job_incomplete, operation_complete, monitor_app_status_for_job_completion


class Test(unittest.TestCase):

    def test_check_environment_variable_read_correctly(self):
        os.environ["PYTHON_JOB_IMAGE_NAME"] = "somePythonJobImageValue:0.0.0"
        self.assertEqual(os.environ["PYTHON_JOB_IMAGE_NAME"], "somePythonJobImageValue:0.0.0")

    def test_delete_job(self):
        job_name = "job-name"
        job_namespace = "job-namespace"

        delete_job_return_value = kubernetes.client.models.V1Status(
            api_version='batch/v1',
            code=None,
            details=None,
            metadata=None,
            reason=None,
            status='Success'
        )

        with mock.patch("kubernetes.client.BatchV1Api.delete_namespaced_job") as kube_api_mock_delete:
            kube_api_mock_delete.return_value = delete_job_return_value
            result = delete_job(job_name, job_namespace)
            args = kube_api_mock_delete.call_args_list
            assert args[0][0][0] == job_name
            assert args[0][0][1] == job_namespace

            delete_job_status = kube_api_mock_delete.return_value.status
            assert delete_job_status == "Success"

    def test_job_incomplete(self):
        job_name = "job-decompress"
        namespace = "test-ns"
        error_msg = "decompress job incomplete"
        app_id = 1
        mock_connect = MagicMock()

        delete_job_return_value = kubernetes.client.models.V1Status(
            api_version='batch/v1',
            code=None,
            details=None,
            metadata=None,
            reason=None,
            status='Success'
        )

        with mock.patch("kubernetes.client.BatchV1Api.delete_namespaced_job") as kube_api_mock_delete:
            with mock.patch('psycopg2.connect') as mock_connect:
                kube_api_mock_delete.return_value = delete_job_return_value
                expected = "Updated application status to: 10"
                mock_cursor = MagicMock()
                mock_cursor.fetchall.return_value = expected
                mock_connect.cursor.return_value = mock_cursor

                job_incomplete(job_name, namespace, error_msg, app_id, mock_connect)

                args = kube_api_mock_delete.call_args_list
                assert args[0][0][0] == job_name
                assert args[0][0][1] == namespace

    def test_operation_complete(self):
        operation = "parser"
        job_name = "job-parser"
        current_status = 40
        namespace = "test-ns"

        delete_job_return_value = kubernetes.client.models.V1Status(
            api_version='batch/v1',
            code=None,
            details=None,
            metadata=None,
            reason=None,
            status='Success'
        )

        with mock.patch("kubernetes.client.BatchV1Api.delete_namespaced_job") as kube_api_mock_delete:
            kube_api_mock_delete.return_value = delete_job_return_value
            result = operation_complete(operation, job_name, current_status, namespace)

            args = kube_api_mock_delete.call_args_list
            assert args[0][0][0] == job_name
            assert args[0][0][1] == namespace

            self.assertEqual(True, result)

    @mock.patch.object(dbFunctions, 'get_conn')
    def test_monitor_app_status_for_job_completion_wrong_operation(self, mock_get):
        operation = "unknown-operation"
        job_name = "job-unknown"
        namespace = "test-ns"
        app_id = 2
        artifact_id = 3

        delete_job_return_value = kubernetes.client.models.V1Status(
            api_version='batch/v1',
            code=None,
            details=None,
            metadata=None,
            reason=None,
            status='Success'
        )

        with mock.patch("kubernetes.client.BatchV1Api.delete_namespaced_job") as kube_api_mock_delete:
            with mock.patch.dict(os.environ, {"PYTHON_JOB_POLLING_TIME": "1200"}):
                kube_api_mock_delete.return_value = delete_job_return_value
                monitor_app_status_for_job_completion(operation, job_name, namespace, app_id, artifact_id)

                args = kube_api_mock_delete.call_args_list
                assert args[0][0][0] == job_name
                assert args[0][0][1] == namespace

    def test_monitor_app_status_for_job_completion_parser_operation_complete(self):
        operation = "parser"
        job_name = "job-parser"
        namespace = "test-ns"
        app_id = 2
        artifact_id = 3

        delete_job_return_value = kubernetes.client.models.V1Status(
            api_version='batch/v1',
            code=None,
            details=None,
            metadata=None,
            reason=None,
            status='Success'
        )

        with mock.patch("kubernetes.client.BatchV1Api.delete_namespaced_job") as kube_api_mock_delete:
            with mock.patch.dict(os.environ, {"PYTHON_JOB_POLLING_TIME": "1200"}):
                with mock.patch("dbFunctions.get_conn") as mock_get_connection:
                    with mock.patch("dbFunctions.get_app_status") as mock_get_app_status:
                        kube_api_mock_delete.return_value = delete_job_return_value
                        mock_get_app_status.return_value = 40
                        monitor_app_status_for_job_completion(operation, job_name, namespace, app_id, artifact_id)

                        args_delete_job_call = kube_api_mock_delete.call_args_list
                        assert args_delete_job_call[0][0][0] == job_name
                        assert args_delete_job_call[0][0][1] == namespace

                        mock_get_app_status.assert_called_once()

                        args = mock_get_app_status.call_args_list
                        assert args[0][0][0] == app_id

    def test_monitor_app_status_for_job_completion_parser_operation_failure(self):
        operation = "parser"
        job_name = "job-parser"
        namespace = "test-ns"
        app_id = 2
        artifact_id = 3

        delete_job_return_value = kubernetes.client.models.V1Status(
            api_version='batch/v1',
            code=None,
            details=None,
            metadata=None,
            reason=None,
            status='Success'
        )

        with mock.patch("kubernetes.client.BatchV1Api.delete_namespaced_job") as kube_api_mock_delete:
            with mock.patch.dict(os.environ, {"PYTHON_JOB_POLLING_TIME": "1200"}):
                with mock.patch("dbFunctions.get_conn") as mock_get_connection:
                    with mock.patch("dbFunctions.get_app_status") as mock_get_app_status:
                        kube_api_mock_delete.return_value = delete_job_return_value
                        mock_get_app_status.return_value = 10
                        monitor_app_status_for_job_completion(operation, job_name, namespace, app_id, artifact_id)

                        args_delete_job_call = kube_api_mock_delete.call_args_list
                        assert args_delete_job_call[0][0][0] == job_name
                        assert args_delete_job_call[0][0][1] == namespace

                        mock_get_app_status.assert_called_once()

                        args = mock_get_app_status.call_args_list
                        assert args[0][0][0] == app_id

    def test_monitor_app_status_for_job_completion_push_helm_operation_complete(self):
        operation = "push-helm"
        job_name = "job-push-helm"
        namespace = "test-ns"
        app_id = 1
        artifact_id = 3

        delete_job_return_value = kubernetes.client.models.V1Status(
            api_version='batch/v1',
            code=None,
            details=None,
            metadata=None,
            reason=None,
            status='Success'
        )

        with mock.patch("kubernetes.client.BatchV1Api.delete_namespaced_job") as kube_api_mock_delete:
            with mock.patch.dict(os.environ, {"PYTHON_JOB_POLLING_TIME": "1200"}):
                with mock.patch("dbFunctions.get_artifact_status") as mock_get_artifact_status:
                    with mock.patch("dbFunctions.get_conn") as mock_get_connection:
                        kube_api_mock_delete.return_value = delete_job_return_value
                        mock_get_artifact_status.return_value = 30
                        monitor_app_status_for_job_completion(operation, job_name, namespace, app_id, artifact_id)

                        args_delete_job_call = kube_api_mock_delete.call_args_list
                        assert args_delete_job_call[0][0][0] == job_name
                        assert args_delete_job_call[0][0][1] == namespace

                        mock_get_artifact_status.assert_called_once()

                        args = mock_get_artifact_status.call_args_list
                        assert args[0][0][0] == artifact_id

    @patch('os.environ')
    @patch('os.getenv')
    @patch.object(jobService, 'create_job')
    @patch.object(jobService, 'monitor_app_status_for_job_completion')
    @patch.object(kubernetes.client, 'V1Job')
    @patch.object(kubernetes.client, 'V1JobSpec')
    @patch.object(kubernetes.client, 'V1PodTemplateSpec')
    @patch.object(kubernetes.client, 'V1Affinity')
    @patch.object(kubernetes.client, 'V1Volume')
    @patch.object(kubernetes.client, 'V1Container')
    def test_create_job_object(self, mock_v1container, mock_v1volume, mock_v1affinity, mock_v1podtemplatespec,
                               mock_v1jobspec, mock_v1job, mock_monitor, mock_create_job, mock_getenv, mock_environ):
        mock_getenv.return_value = '{"key":"value"}'
        mock_environ.return_value = None

        jobService.create_job_object('python3 command_line.py  /helm/location 1 test-app',
                                     'push-image', 'image.tar', 'test_ns', 1200, 'test-app', 1)

        mock_create_job.assert_called()
        job_name = jobService.jobNames[0]
        self.assertTrue(job_name.startswith('job-push-image'))
        mock_v1container.assert_called_with(name=ANY, image=ANY, command=ANY,
                                            args=['python3 command_line.py  /helm/location 1 test-app'],
                                            env=ANY, volume_mounts=ANY)
        mock_v1volume.assert_called()
        mock_v1affinity.assert_called()
        mock_v1podtemplatespec.assert_called()
        mock_v1jobspec.assert_called()
        mock_v1job.assert_called()
        mock_monitor.assert_called()

    @patch('os.environ')
    @patch('os.getenv')
    @patch.object(jobService, 'create_job')
    @patch.object(jobService, 'monitor_app_status_for_job_completion')
    @patch.object(kubernetes.client, 'V1Job')
    @patch.object(kubernetes.client, 'V1JobSpec')
    @patch.object(kubernetes.client, 'V1PodTemplateSpec')
    @patch.object(kubernetes.client, 'V1Affinity')
    @patch.object(kubernetes.client, 'V1Volume')
    @patch.object(kubernetes.client, 'V1Container')
    def test_create_job_object_direct_streaming(self, mock_v1container, mock_v1volume, mock_v1affinity, mock_v1podtemplatespec,
                               mock_v1jobspec, mock_v1job, mock_monitor, mock_create_job, mock_getenv, mock_environ):
        mock_getenv.return_value = '{"key":"value"}'
        mock_environ.return_value = None

        with patch('helperFunctions.get_environment_variable_value', return_value='direct'):
            jobService.create_job_object('python3 command_line.py  /helm/location 1 test-app',
                                     'push-image', 'image.tar', 'test_ns', 1200, 'test-app', 1)

        mock_create_job.assert_called()
        job_name = jobService.jobNames[0]
        self.assertTrue(job_name.startswith('job-push-image'))
        mock_v1container.assert_called_with(name=ANY, image=ANY, command=ANY,
                                            args=['python3 command_line.py  /helm/location 1 test-app'],
                                            env=ANY, volume_mounts=ANY)
        mock_v1volume.assert_called()
        mock_v1affinity.assert_called()
        mock_v1podtemplatespec.assert_called()
        mock_v1jobspec.assert_called()
        mock_v1job.assert_called()
        mock_monitor.assert_called()


    @patch.object(jobService, 'load_incluster_config')
    @patch.object(kubernetes.client.BatchV1Api, 'create_namespaced_job')
    def test_create_job(self, mock_v1api, mock_load_config):
        # mock_v1api.return_value = MagicMock()

        jobService.create_job('dummy_job', 'test_ns')
        mock_v1api.assert_called_with(body='dummy_job', namespace='test_ns')

    @patch.object(kubernetes.config, 'load_incluster_config')
    @patch.object(kubernetes.client, 'BatchV1Api')
    def test_main_init(self, mock_k8sclient, mock_k8sload):
        runpy.run_module('jobService', run_name='__main__')

        mock_k8sload.assert_called()
        mock_k8sclient.assert_called()

    @patch('os.environ')
    @patch('os.getenv')
    def test_env_vars(self, mock_getenv, mock_environ):
        mock_getenv.return_value = '{"key":"value"}'
        mock_environ.return_value = None

        envs = jobService.env_vars('push-image', 'test_ns', 1, 'app-onboarding')

        self.assertIsNotNone(envs)
        self.assertEqual(24, len(envs))

    @patch.object(dbFunctions, 'get_app_events')
    @patch.object(dbFunctions, 'get_conn')
    def test_get_app_events(self, mock_connect, mock_get_app_events):
        mock_connect.return_vlue = MagicMock()  # Mock the db connection

        jobService.get_app_events(1)
        mock_get_app_events.assert_called_with(1, ANY)

    @patch("kubernetes.client.BatchV1Api.delete_namespaced_job")
    def test_delete_jobs(self, mock_delete):
        mock_delete.return_value = None
        jobService.jobNames = ["job-name", "job-name2", "job-name3", "job-name4"]

        jobService.delete_jobs("test_ns")
        mock_delete.assert_called()
        self.assertEqual(mock_delete.call_count, 4)

    @patch.object(jobService, 'job_incomplete')
    @patch.object(jobService, 'delete_job')
    @patch.object(jobService, 'get_app_events')
    @patch.object(dbFunctions, 'get_app_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_monitor_decompressor_status_30(self, mock_connect, mock_status, mock_events, mock_delete, mock_incomplete):
        mock_connect.return_vlue = MagicMock()  # Mock the db connection
        mock_status.return_value = 30
        mock_delete.return_value = None
        os.environ['PYTHON_JOB_POLLING_TIME'] = '20'

        jobService.monitor_app_status_for_job_completion("decompressor", "job_decompressor_30", "test_ns", 1, 1)
        mock_delete.assert_called_with("job_decompressor_30", "test_ns")
        assert not mock_incomplete.called, 'method should not have been called'

    @patch.object(jobService, 'job_incomplete')
    @patch.object(jobService, 'delete_job')
    @patch.object(jobService, 'get_app_events')
    @patch.object(dbFunctions, 'get_app_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_monitor_decompressor_status_10(self, mock_connect, mock_status, mock_events, mock_delete, mock_incomplete):
        mock_connect.return_vlue = MagicMock()  # Mock the db connection
        mock_status.return_value = 10
        mock_delete.return_value = None
        os.environ['PYTHON_JOB_POLLING_TIME'] = '20'

        jobService.monitor_app_status_for_job_completion("decompressor", "job_decompressor_10", "test_ns", 1, 1)
        mock_delete.assert_called_with("job_decompressor_10", "test_ns")
        assert not mock_incomplete.called, 'method should not have been called'

    @patch.object(jobService, 'job_incomplete')
    @patch.object(jobService, 'delete_job')
    @patch.object(jobService, 'get_app_events')
    @patch.object(dbFunctions, 'get_artifact_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_monitor_pushhelm_status_30(self, mock_connect, mock_status, mock_events, mock_delete, mock_incomplete):
        mock_connect.return_vlue = MagicMock()  # Mock the db connection
        mock_status.return_value = 30
        mock_delete.return_value = None
        os.environ['PYTHON_JOB_POLLING_TIME'] = '20'

        jobService.monitor_app_status_for_job_completion("push-helm", "job_pushhelm_30", "test_ns", 1, 1)
        mock_delete.assert_called_with("job_pushhelm_30", "test_ns")
        assert not mock_incomplete.called, 'method should not have been called'

    @patch.object(jobService, 'job_incomplete')
    @patch.object(jobService, 'delete_job')
    @patch.object(jobService, 'get_app_events')
    @patch.object(dbFunctions, 'get_artifact_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_monitor_pushhelm_status_10(self, mock_connect, mock_status, mock_events, mock_delete, mock_incomplete):
        mock_connect.return_vlue = MagicMock()  # Mock the db connection
        mock_status.return_value = 10
        mock_delete.return_value = None
        os.environ['PYTHON_JOB_POLLING_TIME'] = '20'

        jobService.monitor_app_status_for_job_completion("push-helm", "job_pushhelm_10", "test_ns", 1, 1)
        mock_delete.assert_called_with("job_pushhelm_10", "test_ns")
        assert not mock_incomplete.called, 'method should not have been called'

    @patch.object(jobService, 'job_incomplete')
    @patch.object(jobService, 'operation_complete')
    @patch.object(jobService, 'get_app_events')
    @patch.object(dbFunctions, 'get_app_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_monitor_maxpoll(self, mock_connect, mock_status, mock_events, mock_complete, mock_incomplete):
        mock_connect.return_vlue = MagicMock()  # Mock the db connection
        mock_status.return_value = 20
        mock_complete.return_value = True
        os.environ['PYTHON_JOB_POLLING_TIME'] = '5'

        jobService.monitor_app_status_for_job_completion("decompressor", "job_decompressor_30", "test_ns", 1, 1)
        assert not mock_complete.called, 'method should not have been called'
        mock_incomplete.assert_called_with("job_decompressor_30", "test_ns", "Job job_decompressor_30 is not complete after 1.0 retries", 1, ANY)
