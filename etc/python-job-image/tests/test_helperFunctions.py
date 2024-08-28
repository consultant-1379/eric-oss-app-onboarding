#
# COPYRIGHT Ericsson 2022
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
import unittest
from unittest import mock
import helperFunctions as helperFunctions


class Test(unittest.TestCase):

    @mock.patch.dict(os.environ, {'SERVICE_MESH_ENABLED': 'true'})
    def test_terminate_sidecar_service_mesh_enabled_succesful(self):
        with mock.patch("requests.post") as request_mock:
            request_mock.return_value.status_code = 200
            helperFunctions.stop_istio_proxy()
            self.assertEqual(request_mock.call_count, 1)

    @mock.patch.dict(os.environ, {'SERVICE_MESH_ENABLED': 'false'})
    def test_terminate_sidecar_service_mesh_disabled(self):
        with mock.patch("requests.post") as request_mock:
            helperFunctions.stop_istio_proxy()
            self.assertEqual(request_mock.call_count, 0)

    def test_get_environment_value(self):
        os.environ["LOGS_STREAMING_METHOD"] = "direct"
        env_value = helperFunctions.get_environment_variable_value("LOGS_STREAMING_METHOD", "indirect")
        self.assertEqual(env_value, "direct")

    def test_get_environment_default_value(self):
        env_value = helperFunctions.get_environment_variable_value("LOGS_STREAMING_METHOD", "indirect")
        self.assertEqual(env_value, "indirect")
