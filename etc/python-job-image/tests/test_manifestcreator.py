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

import json
import os
import unittest
from unittest.mock import patch

import manifestcreator

path_to_file = ""

class Test(unittest.TestCase):

    def setUp(self):
        global path_to_file
        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        path_to_file = file_path

    def test_get_size_of(self):
        test_file_path = path_to_file + '/testFiles/test'
        mc = manifestcreator.ManifestCreator('/path/to/conf', '/path/to/layer')
        size = mc.get_size_of(test_file_path)

        self.assertIsNotNone(size)
        self.assertGreater(size, 0)

    def test_get_sha_256_properly_formatted(self):
        test_file_path = path_to_file + '/testFiles/test'
        mc = manifestcreator.ManifestCreator('/path/to/conf', '/path/to/layer')
        sha = mc.get_sha_256_of_file(test_file_path)

        self.assertIsNotNone(sha)
        self.assertGreater(len(sha), 0)
        self.assertTrue(True, sha.__contains__('sha256:'))

    @patch.object(manifestcreator.ManifestCreator, 'get_size_of')
    @patch.object(manifestcreator.ManifestCreator, 'get_sha_256_properly_formatted')
    def test_create_json2(self, fake_get_sha_256_properly_formatted, fake_get_size_of):
        fake_get_size_of.return_value = 10
        fake_get_sha_256_properly_formatted.return_value = 'sha256:sd5g4sd65g4s6g4s6dg46s4g6s4dg4'
        mc = manifestcreator.ManifestCreator('/path/to/conf', '/path/to/layer')
        json_str = mc.create_json()
        json_obj = json.loads(json_str)

        self.assertIsNotNone(json_str)
        self.assertGreaterEqual(len(json_obj), 0)
        self.assertEqual(2, json_obj['schemaVersion'])
        self.assertEqual(json_obj["config"]["mediaType"],"application/vnd.docker.container.image.v1+json")
