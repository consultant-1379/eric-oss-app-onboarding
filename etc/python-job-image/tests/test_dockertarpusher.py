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

import hashlib
import json
import os
from unittest.mock import MagicMock, patch, ANY

import requests
from pyfakefs import fake_filesystem_unittest

import dbFunctions
import dockertarpusher
import manifestcreator

reg = None
path_to_csar = ''


class Test(fake_filesystem_unittest.TestCase):
    def setUp(self):
        global path_to_csar
        global reg
        mock_connect = MagicMock()
        path = os.path.abspath(__file__) # Get correct path to /tests
        file_path, file_name = os.path.split(path)
        path_to_csar = file_path
        self.setUpPyfakefs()  # set up fake filesystem
        self.fs.add_real_directory(file_path + "/testFiles/DecompressedCSAR")  # Add decompressed csar to filesystem
        self.fs.add_real_directory(file_path + "/testFiles/test")
        reg = dockertarpusher.Registry("test", file_path +
                                       "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/busybox.tar", "1", "1",
                                       mock_connect)  # Set values for dockertarpusher to use

    def test_extract_from_tar_and_get_file(self):
        result = dockertarpusher.Registry.extract_from_tar_and_get_file(reg, reg.image_path, "manifest.json")
        manifest_file = json.load(result)  # Retreived Manifest
        global repo_tags
        expected_tags = ['busybox:latest']
        for DockerImage in manifest_file:
            repo_tags = DockerImage["RepoTags"]  # Loop over manifest and compare the tags
        print(repo_tags, expected_tags)
        self.assertEqual(repo_tags, expected_tags)

    def test_extract_tar_file(self):
        result = dockertarpusher.Registry.extract_tar_file(reg, "tmpDir")
        print(result)
        self.assertIs(result, True)  # Returns true if successful

    @patch.object(requests, 'put')
    def test_push_manifest(self, mockput):
        mockresponse = mockput
        mockput.status_code = 201
        mockput.return_value = mockresponse
        result = dockertarpusher.Registry.push_manifest(reg, "test", "test", "10")
        print(result)
        self.assertIs(result, True)  # Returns true if successful , status code: 201

    @patch.object(requests, 'put')
    def test_push_manifest_fail(self, mockput):
        mockresponse = mockput
        mockput.status_code = 404
        mockput.return_value = mockresponse
        result = dockertarpusher.Registry.push_manifest(reg, "test", "test", "10")
        print(result)
        self.assertIs(result, False)  # Returns false if unsuccessful , status code: 404

    def test_get_image_tag(self):
        result = dockertarpusher.Registry.get_image_tag(reg, "example:10")  # Split  image name & tag
        print("Image, Tag & Name = " + str(result))
        self.assertEqual(result[0], "example")
        self.assertEqual(result[1], "10")
        self.assertEqual(result[2], "example")

    def test_get_image_tag_with_path(self):
        result = dockertarpusher.Registry.get_image_tag(reg, "proj-eric-oss-drop/eric-oss-cnr5gassist:1.1.108-1")  # Split  image name & tag
        self.assertEqual(result[0], "proj-eric-oss-drop/eric-oss-cnr5gassist")
        self.assertEqual(result[1], "1.1.108-1")
        self.assertEqual(result[2], "eric-oss-cnr5gassist")

    def test_get_image_tag_fail(self):
        try:
            result = dockertarpusher.Registry.get_image_tag(reg, "example10")

        except IndexError as error:
            print("Index Error")  # Failed to split the image name & tag
            pass

    @patch.object(requests, 'post')
    def test_start_pushing(self, mockpost):
        mockresponse = mockpost
        mockpost.status_code = 202
        mockpost.return_value = mockresponse
        result = dockertarpusher.Registry.start_pushing(reg, "testRepo")
        print(result)
        self.assertIs(result[0], True)  # Returns true if status code is 202

    @patch.object(requests, 'post')
    def test_start_pushing_fail(self, mockpost):
        mockresponse = mockpost
        mockpost.status_code = 404
        mockpost.return_value = mockresponse
        result = dockertarpusher.Registry.start_pushing(reg, "testRepo")
        print(result)
        self.assertIs(result[0], False)  # Returns false if status code is 404

    @patch.object(dockertarpusher.Registry, 'update_image_error_status')
    def test_get_images_from_tar_null_repotags(self, mock_update_image_error_status):
        mock_connect = MagicMock()  # Mock db connection
        mock_update_image_error_status.return_value = MagicMock()

        path = os.path.abspath(__file__)  # Get correct path to /tests
        file_path, file_name = os.path.split(path)
        reg_no_tags = dockertarpusher.Registry("test",
                                               file_path +
                                               "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/" +
                                               "busybox_null_repotags.tar",
                                               "2",
                                               "2",
                                               mock_connect)

        dockertarpusher.Registry.get_images_from_tar(reg_no_tags)

        mock_update_image_error_status.assert_called_with("Processing image error: RepoTags in manifest are empty")

    @patch.object(dockertarpusher.Registry, 'update_image_error_status')
    def test_get_images_from_tar_null_layers(self, mock_update_image_error_status):
        mock_connect = MagicMock()  # Mock db connection
        mock_update_image_error_status.return_value = MagicMock()

        path = os.path.abspath(__file__)  # Get correct path to /tests
        file_path, file_name = os.path.split(path)
        reg_no_layers = dockertarpusher.Registry("test",
                                                 file_path +
                                                 "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/" +
                                                 "busybox_null_layers.tar",
                                                 "2",
                                                 "2",
                                                 mock_connect)

        dockertarpusher.Registry.get_images_from_tar(reg_no_layers)

        mock_update_image_error_status.assert_called_with("Processing image error: layers in manifest are empty")

    @patch.object(dockertarpusher.Registry, 'update_image_error_status')
    def test_get_images_from_tar_null_config(self, mock_update_image_error_status):
        mock_connect = MagicMock()  # Mock db connection
        mock_update_image_error_status.return_value = MagicMock()

        path = os.path.abspath(__file__)  # Get correct path to /tests
        file_path, file_name = os.path.split(path)
        reg_no_config = dockertarpusher.Registry("test",
                                                 file_path +
                                                 "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/" +
                                                 "busybox_null_config.tar",
                                                 "2",
                                                 "2",
                                                 mock_connect)

        dockertarpusher.Registry.get_images_from_tar(reg_no_config)

        mock_update_image_error_status.assert_called_with("Processing image error: Config in manifest is empty")

    @patch.object(dockertarpusher.Registry, 'update_image_error_status')
    @patch.object(dockertarpusher.Registry, 'process_image')
    def test_get_images_from_tar_multiple_images(self, mock_process_image, mock_tar_pusher):
        mock_connect = MagicMock()
        dbFunctions.get_conn = mock_connect
        mock_tar_pusher.return_value = MagicMock()
        mock_process_image.return_value = MagicMock()

        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        reg_multiple_images = dockertarpusher.Registry("test",
                                                       file_path +
                                                       "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/" +
                                                       "busybox_multiple_images.tar",
                                                       "3",
                                                       "3",
                                                       mock_connect)
        try:
            dockertarpusher.Registry.get_images_from_tar(reg_multiple_images)
        except Exception as error:
            print(f"Error: {error}")
            raise
        else:
            pass

    @patch.object(dockertarpusher.Registry, 'update_image_error_status')
    @patch.object(dockertarpusher.Registry, 'process_image')
    def test_get_images_from_tar_process_image_error(self, mock_process_image, mock_update_image_error_status):
        mock_connect = MagicMock()
        dbFunctions.get_conn = mock_connect
        mock_update_image_error_status.return_value = MagicMock()
        mock_process_image.return_value = False

        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        reg_push_error = dockertarpusher.Registry("test",
                                                  file_path +
                                                  "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/" +
                                                  "busybox_multiple_images.tar",
                                                  "3",
                                                  "3",
                                                  mock_connect)
        dockertarpusher.Registry.get_images_from_tar(reg_push_error)

        mock_update_image_error_status.assert_called_with("Processing image error: Image Upload error, artifact id " +
                                                          reg_push_error.artifact_id)

    @patch.object(dockertarpusher.Registry, 'process_image')
    def test_get_images_from_tar_error(self, mock_process_image):
        mock_connect = MagicMock()
        dbFunctions.get_conn = mock_connect
        mock_process_image.side_effect = Exception('Dummy error')

        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        reg_push_error = dockertarpusher.Registry("test",
                                                  file_path +
                                                  "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/" +
                                                  "busybox_multiple_images.tar",
                                                  "3",
                                                  "3",
                                                  mock_connect)
        dockertarpusher.Registry.get_images_from_tar(reg_push_error)

        self.assertRaises(Exception)

    def test_get_sha_256_of_file(self):
        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        self.fs.add_real_file(file_path + "/testFiles/test", False, "/tmp/test")

        hexsha = dockertarpusher.Registry.get_sha_256_of_file(reg, file_path + "/testFiles/test")
        self.assertIsNotNone(hexsha)
        self.assertGreater(len(hexsha), 0)

    def test_read_in_chunks(self):
        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        self.fs.add_real_file(file_path + "/testFiles/test", False, "/tmp/test")
        f = open('/tmp/test', "rb")
        sha256hash = hashlib.sha256()

        chunk = dockertarpusher.Registry.read_in_chunks(reg, f, sha256hash)
        f.close()
        self.assertGreater(len(str(chunk)), 1)

    @patch.object(dockertarpusher.Registry, "update_image_error_status")
    @patch.object(dockertarpusher.Registry, "process_image")
    def test_getImageFromTar(self, mock_process_image, mock_updateImgErrorStatus):
        mock_connect = MagicMock()
        path = os.path.abspath(__file__)
        file_path, file_name = os.path.split(path)
        reg = dockertarpusher.Registry(
            "test", file_path +
            "/testFiles/DecompressedCSAR/OtherDefinitions/ASD/images/busybox_null_repotags.tar",
            "1",
            "1",
            mock_connect)

        dockertarpusher.Registry.get_images_from_tar(reg)
        mock_updateImgErrorStatus.assert_called()
        mock_process_image.assert_not_called()

    @patch.object(dbFunctions, 'update_artifact')
    def test_process_image_no_repotags(self, mock_update_artifact):
        repo_tags = []
        config_file = None
        layers = []

        response = dockertarpusher.Registry.process_image(reg, repo_tags, config_file, layers)
        self.assertTrue(response)
        assert not mock_update_artifact.called, 'method should not have been called'

    @patch.object(dockertarpusher.Registry, 'push_layer')
    @patch.object(dockertarpusher.Registry, 'start_pushing')
    @patch.object(dockertarpusher.Registry, 'extract_tar_file')
    @patch.object(dbFunctions, 'update_artifact')
    @patch.object(dbFunctions, 'update_artifact_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_process_image_one_repotag_failed_push(self, mock_connect, mock_update_artifact_status, mock_update_artifact,
                                       mock_extract_tar, mock_pushing, mock_push_layer):
        repo_tags = ["proj-eric-oss-drop/eric-oss-hello-world-go-app:0.0.5-7"]
        config_file = 'test'
        layers = ["ee4335802faa/layer.tar"]
        mock_pushing.return_value = False, 'http://dummy.upload.url'

        response = dockertarpusher.Registry.process_image(reg, repo_tags, config_file, layers)

        self.assertFalse(response)
        mock_pushing.assert_called()
        assert not mock_push_layer.called, 'method should not have been called'
        mock_update_artifact_status.assert_called_with(10, "", '1', ANY)

    @patch.object(dockertarpusher.Registry, 'push_layer')
    @patch.object(dockertarpusher.Registry, 'start_pushing')
    @patch.object(dockertarpusher.Registry, 'extract_tar_file')
    @patch.object(dbFunctions, 'update_artifact')
    @patch.object(dbFunctions, 'update_artifact_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_process_image_one_repotag_nolayers(self, mock_connect, mock_update_artifact_status, mock_update_artifact,
                                       mock_extract_tar, mock_pushing, mock_push_layer):
        repo_tags = ["proj-eric-oss-drop/eric-oss-hello-world-go-app:0.0.5-7"]
        config_file = 'test'
        layers = []
        mock_pushing.return_value = False, 'http://dummy.upload.url'

        response = dockertarpusher.Registry.process_image(reg, repo_tags, config_file, layers)

        self.assertFalse(response)
        mock_pushing.assert_called()
        assert not mock_update_artifact_status.called, 'method should not have been called'
        assert not mock_push_layer.called, 'method should not have been called'

    @patch.object(dockertarpusher.Registry, 'push_manifest')
    @patch.object(manifestcreator.ManifestCreator, 'create_json')
    @patch('manifestcreator.ManifestCreator')
    @patch.object(dockertarpusher.Registry, 'push_config')
    @patch.object(dockertarpusher.Registry, 'push_layer')
    @patch.object(dockertarpusher.Registry, 'start_pushing')
    @patch.object(dockertarpusher.Registry, 'extract_tar_file')
    @patch.object(dbFunctions, 'update_artifact')
    @patch.object(dbFunctions, 'update_artifact_status')
    @patch.object(dbFunctions, 'get_conn')
    def test_process_image_one_repotag_success(self, mock_connect, mock_update_artifact_status, mock_update_artifact,
                                       mock_extract_tar, mock_pushing, mock_push_layer, mock_push_conf,
                                       mock_man_creator, mock_man_creator_json, mock_push_man):
        repo_tags = ["proj-eric-oss-drop/eric-oss-hello-world-go-app:0.0.5-7"]
        config_file = 'test'
        layers = ["ee4335802faa/layer.tar"]
        mock_pushing.return_value = True, 'http://dummy.upload.url'
        mock_push_man.return_value = True
        mock_push_conf.return_value = MagicMock()
        mock_man_creator_json.return_value = None

        response = dockertarpusher.Registry.process_image(reg, repo_tags, config_file, layers)

        self.assertTrue(response)
        mock_pushing.assert_called()
        mock_push_layer.assert_called()
        mock_push_conf.assert_called()
        mock_push_man.assert_called()
        mock_update_artifact.assert_called()

    @patch('dbFunctions.app_error')
    @patch('dbFunctions.update_artifact_status')
    @patch('dbFunctions.get_conn')
    def test_update_image_error_status(self, mock_connect, mock_update_artifact_status, mock_app_err):
        err_msg = "Processing image error: RepoTags in manifest are empty"
        mock_connect = MagicMock()

        dockertarpusher.Registry.update_image_error_status(reg, err_msg)

        mock_update_artifact_status.assert_called_with(10, "", '1', ANY)
        mock_app_err.assert_called_with(10, err_msg, '1', ANY)

    @patch('builtins.open')
    @patch('hashlib.sha256')
    @patch.object(dockertarpusher.Registry, 'read_in_chunks')
    def test_chunked_upload_nodata(self, mock_read_chunk, mock_sha256, mock_open):
        self.fs.add_real_file(path_to_csar + "/testFiles/test", False, "/tmp/test")
        mock_read_chunk.return_value = MagicMock()

        dockertarpusher.Registry.chunked_upload(reg, "/tmp/test", 'http://localhost:8080/upload')

        mock_open.assert_called()
        mock_sha256.assert_called()

    @patch('requests.patch')
    @patch('builtins.open')
    @patch('hashlib.sha256')
    @patch.object(dockertarpusher.Registry, 'read_in_chunks')
    def test_chunked_upload_patch(self, mock_read_chunk, mock_sha256, mock_open, mock_patch):
        self.fs.add_real_file(path_to_csar + "/testFiles/test", False, "/tmp/test")
        mock_read_chunk.return_value = ['test1', 'test2']

        dockertarpusher.Registry.chunked_upload(reg, "/tmp/test", 'http://localhost:8080/upload')

        mock_patch.assert_called()

    @patch('os.stat')
    @patch('requests.patch')
    @patch('requests.put')
    @patch('builtins.open')
    @patch('hashlib.sha256')
    @patch.object(dockertarpusher.Registry, 'read_in_chunks')
    def test_chunked_upload_put(self, mock_read_chunk, mock_sha256, mock_open, mock_put, mock_patch, mock_st_size):
        self.fs.add_real_file(path_to_csar + "/testFiles/test", False, "/tmp/test")
        mock_read_chunk.return_value = ['test1', 'test2']
        mock_st_size.return_value.st_size = 5

        dockertarpusher.Registry.chunked_upload(reg, "/tmp/test", 'http://localhost:8080/upload')

        mock_put.assert_called()
        mock_patch.assert_called()

    @patch('requests.patch')
    @patch('builtins.open')
    @patch('hashlib.sha256')
    @patch.object(dockertarpusher.Registry, 'read_in_chunks')
    def test_chunked_upload_exception(self, mock_read_chunk, mock_sha256, mock_open, mock_patch):
        self.fs.add_real_file(path_to_csar + "/testFiles/test", False, "/tmp/test")
        mock_read_chunk.return_value = ['test1', 'test2']
        mock_patch.side_effect = Exception()

        res = dockertarpusher.Registry.chunked_upload(reg, "/tmp/test", 'http://localhost:8080/upload')

        mock_patch.assert_called()
        self.assertFalse(res)
