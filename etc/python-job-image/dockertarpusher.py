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
import tarfile
import tempfile
import constants

import requests
from requests.auth import HTTPBasicAuth

import dbFunctions as db
from manifestcreator import ManifestCreator

from loggingConfig import get_logger

logger = get_logger(__name__)


class Registry:
    os.chdir("/tmp")

    def __init__(self, registry_path, image_path, app_id, artifact_id, conn, stream=False, auth = {'login': None, 'password': None, 'verify': True}):
        self.registry_path = registry_path
        self.image_path = image_path
        self.login = auth['login']
        self.password = auth['password']
        self.auth = None
        self.stream = stream
        self.ssl_verify = auth['verify']
        self.app_id = app_id
        self.artifact_id = artifact_id
        self.conn = conn
        if self.login:
            self.auth = HTTPBasicAuth(self.login, self.password)

    def get_manifest(self):
        return self.extract_from_tar_and_get_file(self.image_path, "manifest.json")

    def extract_from_tar_and_get_file(self, tar_path, file_to_extract):
        logger.info("extract_from_tar_and_get_file() extracting file: %s with path: %s", file_to_extract, tar_path)
        manifest = tarfile.open(tar_path)
        manifest_str_file = manifest.extractfile(file_to_extract)
        return manifest_str_file

    def extract_tar_file(self, tmp__dir_name):
        tar = tarfile.open(self.image_path)
        tar.extractall(tmp__dir_name)
        tar.close()
        return True

    def get_images_from_tar(self):
        manifest_file = json.load(self.get_manifest())
        image_artifact_count = 1
        first_artifact_id = self.artifact_id
        total_images = len(manifest_file)
        try:
            for docker_image in manifest_file:
                repo_tags = docker_image["RepoTags"]
                config_file = docker_image["Config"]
                layers = docker_image["Layers"]

                if not repo_tags:
                    self.update_image_error_status("Processing image error: RepoTags in manifest are empty")
                    return
                elif not layers:
                    self.update_image_error_status("Processing image error: layers in manifest are empty")
                    return
                elif not config_file:
                    self.update_image_error_status("Processing image error: Config in manifest is empty")
                    return
                else:
                    if image_artifact_count < total_images:
                        current_artifact_id = db.onboard_artifacts("--", "--", "I", repo_tags[0], 20, self.app_id,
                                                                   db.get_conn())
                        self.artifact_id = str(current_artifact_id)
                    else:
                        self.artifact_id = first_artifact_id

                    if self.process_image(repo_tags, config_file, layers):
                        logger.debug("get_images_from_tar() successfully transferred artifact id: %s ", self.artifact_id)
                        db.update_single_artifact_status(constants.ARTIFACT_STATUS_COMPLETED, self.artifact_id, db.get_conn())
                    else:
                        logger.error("get_images_from_tar() failed to process artifact with id: %s", self.artifact_id)
                        self.update_image_error_status("Processing image error: Image Upload error, artifact id " + self.artifact_id)
                        db.update_single_artifact_status(constants.ARTIFACT_STATUS_FAILED, first_artifact_id, db.get_conn())
                        return

                image_artifact_count = image_artifact_count + 1

        except Exception as e:
            logger.error("get_images_from_tar() error: %s", e)
            self.update_artifacts_status([self.artifact_id, first_artifact_id], constants.ARTIFACT_STATUS_FAILED)

    def process_image(self, repo_tags, config_file, layers):

        with tempfile.TemporaryDirectory() as tmpdirname:
            for repo in repo_tags:
                image, tag, artifact_name = self.get_image_tag(repo)
                logger.info("process_image() extracting tar for %s with tag: %s", image, tag)
                self.extract_tar_file(tmpdirname)
                for layer in layers:
                    logger.debug("process_image() starting pushing layer %s", layer)
                    status, url = self.start_pushing(image)
                    if not status:
                        logger.error("process_image() could not start upload, check that the details are correct")
                        artifact_status = constants.ARTIFACT_STATUS_FAILED
                        db.update_artifact_status(artifact_status, "", self.artifact_id, db.get_conn())
                        return False
                    self.push_layer(os.path.join(tmpdirname, layer), image, url)
                logger.debug("process_image() pushing Config")
                status, url = self.start_pushing(image)
                if not status:
                    return False
                self.push_config(os.path.join(tmpdirname, config_file), image, url)
                formatted_layers = []
                for layer in layers:
                    formatted_layers.append(os.path.join(tmpdirname, layer))
                creator = ManifestCreator(os.path.join(tmpdirname, config_file), formatted_layers)
                registry_manifest = creator.create_json()
                logger.debug("process_image() pushing manifest")
                if not self.push_manifest(registry_manifest, image, tag):
                    logger.error("process_image() failed to push manifest")
                    artifact_status = constants.ARTIFACT_STATUS_FAILED
                    db.update_artifact_status(artifact_status, "", self.artifact_id, db.get_conn())
                logger.info("process_image() image pushed")

                location = "/v2/" + image + "/manifests/" + tag
                logger.debug("process_image() updating artifact name, version (tag) and location of artifact_id: %s",
                             self.artifact_id)
                db.update_artifact(artifact_name, tag, location, self.artifact_id, db.get_conn())
        return True

    def push_manifest(self, manifest, image, tag):
        headers = {"Content-Type": "application/vnd.docker.distribution.manifest.v2+json"}
        url = self.registry_path + "/v2/" + image + "/manifests/" + tag
        logger.debug("push_manifest() pushing manifest to url: %s", url)
        r = requests.put(url, headers=headers, data=manifest, auth=self.auth, verify=self.ssl_verify)

        return r.status_code == 201

    def get_image_tag(self, processing):
        splitted = processing.split(":")
        image = splitted[0]
        tag = splitted[1]

        path_name = image.split("/")
        artifact_name = path_name[-1]

        return image, tag, artifact_name

    def start_pushing(self, repository):
        logger.info("start_pushing() upload started")
        r = requests.post(self.registry_path + "/v2/" + repository + "/blobs/uploads/", auth=self.auth,
                          verify=self.ssl_verify)
        upload_url = None
        if r.headers.get("Location", None):
            upload_url = r.headers.get("Location")
        return (r.status_code == 202), upload_url

    def push_layer(self, layer_path, repository, upload_url):
        self.chunked_upload(layer_path, upload_url)

    def push_config(self, layer_path, repository, upload_url):
        self.chunked_upload(layer_path, upload_url)

    def get_sha_256_of_file(self, filepath):
        sha256hash = hashlib.sha256()
        with open(filepath, "rb") as f:
            while True:
                data = f.read(2097152)
                sha256hash.update(data)
                if not data:
                    break
        return sha256hash.hexdigest()

    def read_in_chunks(self, file_object, hashed, chunk_size=2097152):
        while True:
            data = file_object.read(chunk_size)
            hashed.update(data)
            if not data:
                break
            yield data

    def set_auth(self, auth_obj):
        self.auth = auth_obj

    def chunked_upload(self, file, url):
        content_path = os.path.abspath(file)
        content_size = os.stat(content_path).st_size
        f = open(content_path, "rb")
        index = 0
        offset = 0
        headers = {}
        upload_url = url
        sha256hash = hashlib.sha256()

        for chunk in self.read_in_chunks(f, sha256hash):
            if "http" not in upload_url:
                upload_url = self.registry_path + upload_url
            offset = index + len(chunk)
            headers['Content-Type'] = 'application/octet-stream'
            headers['Content-Length'] = str(len(chunk))
            headers['Content-Range'] = '%s-%s' % (index, offset)
            index = offset
            last = False
            if offset == content_size:
                last = True
            try:
                logger.debug("chunked_upload() pushing... %s %", str(round((offset / content_size) * 100, 2)))

                if last:
                    r = requests.put(upload_url + "&digest=sha256:" + str(sha256hash.hexdigest()), data=chunk,
                                     headers=headers, auth=self.auth, verify=self.ssl_verify)

                else:
                    r = requests.patch(upload_url, data=chunk, headers=headers, auth=self.auth, verify=self.ssl_verify)
                    if "Location" in r.headers:
                        upload_url = r.headers["Location"]

            except Exception as e:
                logger.error("chunked_upload() error: %s", e)
                return False
        f.close()
        logger.info("chunked_upload() completed")

    def update_image_error_status(self, image_error):
        artifact_status = constants.ARTIFACT_STATUS_FAILED
        logger.error("update_image_error_status() image push error: %s", image_error)
        db.update_artifact_status(artifact_status, "", self.artifact_id, db.get_conn())
        db.app_error(artifact_status, image_error, self.app_id, db.get_conn())

    def update_artifacts_status(self, artifact_ids, artifact_status):
        if artifact_ids:
            logger.debug("update_artifacts_status() updating status of image artifact ids %s to %s", artifact_ids, str(artifact_status))
            db.update_multiple_artifacts_status(artifact_status, artifact_ids, db.get_conn())