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
from unittest.mock import MagicMock, patch, ANY

from pyfakefs import fake_filesystem_unittest

import dbFunctions
import helperFunctions
from decompressor import decompress

path_to_csar = ""

mock_connect = MagicMock()


class Test(fake_filesystem_unittest.TestCase):

    def setUp(self):
        global path_to_csar
        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        path_to_csar = file_path
        self.setUpPyfakefs()  # set up fake filesystem
        if not self.fs.exists("/tmp"):
            self.fs.add_real_directory("/tmp", False, False, "/tmp")
        self.fs.add_real_file(file_path + "/testFiles/test.csar", False, "/tmp/test.csar")  # Add csar to filesystem
        self.fs.add_real_file(file_path + "/testFiles/test", False, "/tmp/test")

    def test_decompress(self):
        decompress("test.csar", "1", mock_connect)
        self.assertGreaterEqual(set(os.listdir('/tmp')),
                                {'test.csar', "1"})  # check if csar is decompressed and folder with name id is created

    def test_decompress_folders_exist(self):
        decompress("test.csar", "1", mock_connect)
        self.assertEqual(os.listdir('/tmp/1'),
                         ["Definitions", "OtherDefinitions", "Metadata"])  # check if folders exist within directory

    def test_decompress_fail_invalid_file(self):
        try:
            decompress("test", "1", mock_connect)
        except Exception as error:
            pass

    def test_decompress_fail_file_not_found(self):
        try:
            decompress("hello.csar", "1", mock_connect)
        except Exception as error:
            pass

    @patch.object(dbFunctions, 'update_app_status')
    @patch.object(dbFunctions, 'get_conn')
    @patch.object(helperFunctions, 'stop_istio_proxy')
    def test_main_success(self, mock_istio, mock_connect, mock_update_app_status):
        mock_istio.return_value = None
        mock_connect.return_value = MagicMock()
        app_id = '1'
        tgz_name = 'test.csar'
        mock_argv = ['', tgz_name, app_id]

        with patch.object(sys, 'argv', mock_argv):
            runpy.run_module('decompressor', run_name='__main__')

        mock_update_app_status.assert_called_with(30, '1', ANY)
        mock_istio.assert_called()

    @patch.object(dbFunctions, 'app_error')
    @patch.object(dbFunctions, 'get_conn')
    @patch.object(helperFunctions, 'stop_istio_proxy')
    def test_main_error(self, mock_istio, mock_connect, mock_app_error):
        mock_istio.return_value = None
        mock_connect.return_value = MagicMock()
        app_id = '1'
        tgz_name = 'no_file.csar'
        mock_argv = ['', tgz_name, app_id]

        with patch.object(sys, 'argv', mock_argv):
            runpy.run_module('decompressor', run_name='__main__')

        mock_app_error.assert_called_with(10, ('Decompressing error: %s', "[Errno 2] No such file or directory in the "
                                                                          "fake filesystem: '/tmp/no_file.csar'"),
                                          '1', ANY)
        mock_istio.assert_called()
