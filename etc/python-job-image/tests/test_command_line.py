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

import os
import runpy
import sys
from unittest.mock import MagicMock, patch

from pyfakefs import fake_filesystem_unittest

import dbFunctions
import dockertarpusher
import helperFunctions


class Test(fake_filesystem_unittest.TestCase):
    def setUp(self):
        os.environ['CONTAINER_REGISTRY_HOST'] = 'url'
        os.environ['CONTAINER_REGISTRY_USER'] = 'container_user'
        os.environ['CONTAINER_REGISTRY_PASSWORD'] = 'container_pwd'

    @patch.object(dbFunctions, 'get_conn')
    @patch.object(dockertarpusher.Registry, 'get_images_from_tar')
    @patch.object(helperFunctions, 'stop_istio_proxy')
    def test_register_tarpusher(self, mock_istio, mock_get_images_from_tar, mock_connect):
        mock_argv = ["path/location", "push-image", "--noSslVerify", "test_ns", "100", "1", "1"]
        mock_istio.return_value = None
        mock_get_images_from_tar.return_value = None
        mock_connect.return_value = MagicMock()

        with patch.object(sys, 'argv', mock_argv):
            runpy.run_module('command_line', run_name='__main__')
            mock_connect.assert_called()
            mock_get_images_from_tar.assert_called()
            mock_istio.assert_called()

    @patch.object(helperFunctions, 'stop_istio_proxy')
    def test_register_tarpusher_with_less_args(self, mock_istio):
        mock_istio.return_value = None
        with patch.object(sys, 'argv', return_value=None):
            with self.assertRaises(SystemExit):
                runpy.run_module('command_line', run_name='__main__')
        mock_istio.assert_called()

