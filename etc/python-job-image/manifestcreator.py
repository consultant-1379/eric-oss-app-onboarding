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
import hashlib
import json
from loggingConfig import get_logger

logger = get_logger(__name__)


class ManifestCreator:
    def __init__(self, config_path, layers_paths):
        self.config_path = config_path
        self.layers_paths = layers_paths

    def create_json(self):
        result_dict = dict()
        result_dict["schemaVersion"] = 2
        result_dict["mediaType"] = "application/vnd.docker.distribution.manifest.v2+json"
        result_dict["config"] = dict()
        result_dict["config"]["mediaType"] = "application/vnd.docker.container.image.v1+json"

        result_dict["config"]["size"] = self.get_size_of(self.config_path)
        result_dict["config"]["digest"] = self.get_sha_256_properly_formatted(self.config_path)

        result_dict["layers"] = []
        for layer in self.layers_paths:
            layer_dict = dict()
            layer_dict["mediaType"] = "application/vnd.docker.image.rootfs.diff.tar"
            layer_dict["size"] = self.get_size_of(layer)
            layer_dict["digest"] = self.get_sha_256_properly_formatted(layer)
            result_dict["layers"].append(layer_dict)

        logger.info("create_json() Docker manifest JSON created")
        return json.dumps(result_dict)

    def get_size_of(self, path):
        return os.path.getsize(path)

    def get_sha_256_of_file(self, filepath):
        sha256hash = hashlib.sha256()
        with open(filepath, "rb") as f:
            while True:
                data = f.read(65536)
                sha256hash.update(data)
                if not data:
                    break
        return sha256hash.hexdigest()

    def get_sha_256_properly_formatted(self, filepath):
        return "sha256:" + self.get_sha_256_of_file(filepath)
