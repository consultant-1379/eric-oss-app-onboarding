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
import json
import logging
import constants
from logging.config import dictConfig

logs_streaming_method = os.environ.get("LOGS_STREAMING_METHOD", "indirect")

if logs_streaming_method.lower() == "direct" or logs_streaming_method.lower() == "dual":
    logs_target = "/logs/jobs/python-jobs.log"
    if os.path.exists("/logs") and os.path.isdir("/logs"):
        os.makedirs("/logs/jobs", exist_ok=True)
else:
    logs_target = "/tmp/logs/jobs/python-jobs.log"
    os.makedirs("/tmp/logs/jobs", exist_ok=True)

logs_severity = os.environ.get("LOGS_SEVERITY", "INFO")

class JsonFormatter(logging.Formatter):
    def __init__(self, fmt_dict: dict = None, time_format: str = "%Y-%m-%dT%H:%M:%S", msec_format: str = "%s.%03dZ"):
        self.fmt_dict = fmt_dict if fmt_dict is not None else {"message": "message"}
        self.default_time_format = time_format
        self.default_msec_format = msec_format
        self.datefmt = None

    def usesTime(self) -> bool:
        return "asctime" in self.fmt_dict.values()

    def formatMessage(self, record) -> dict:
        return {fmt_key: record.__dict__[fmt_val] for fmt_key, fmt_val in self.fmt_dict.items()}

    def format(self, record) -> str:
        record.message = record.getMessage()

        if self.usesTime():
            record.asctime = self.formatTime(record, self.datefmt)

        message_dict = self.formatMessage(record)

        if record.exc_info and not record.exc_text:
            record.exc_text = self.formatException(record.exc_info)

        if record.exc_text:
            message_dict["exc_info"] = record.exc_text

        if record.stack_info:
            message_dict["stack_info"] = self.formatStack(record.stack_info)

        return json.dumps(message_dict, default=str)


class CustomFilter(logging.Filter):
    def filter(self, record):
        record.namespace = os.getenv("NAMESPACE")
        record.pod_name = os.getenv("POD_NAME")
        record.container_name = os.getenv("CONTAINER_NAME")
        record.app_id = os.getenv("APP_ID")
        record.service_id = os.getenv("SERVICE_ID")
        return True


def get_logger(class_name):
    logger = logging.getLogger(class_name)
    custom_filter = CustomFilter()
    logger.addFilter(custom_filter)
    return logger

def get_logger_with_streaming_method(method):
    return {
        "": {
            "handlers": method,
            "level": logs_severity,
            "propagate": False
        }
    }

LOGGING_SCHEMA = {
    "version": 1,
    "formatters": {
        "standard": {
            "()": JsonFormatter,
            "fmt_dict": {
                "timestamp": "asctime",
                "message": "message",
                "severity": "levelname",
                "logger": "name",
                "thread": "thread",
                "namespace": "namespace",
                "service_id": "service_id",
                "pod_name": "pod_name",
                "container_name": "container_name",
                "app_id": "app_id"
            },
        }
    },
    "handlers": {
        "file": {
            "class": "logging.handlers.RotatingFileHandler",
            "formatter": "standard",
            "level": logs_severity,
            "filename": logs_target,
            "mode": "a",
            "encoding": "utf-8",
            "maxBytes": constants.MAX_BYTES_PYTHON_LOGS,
            "backupCount": 4
        },
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "standard",
            "level": logs_severity,
            "stream": "ext://sys.stdout"
        }
    }
}

if logs_streaming_method == "dual" or logs_streaming_method == "indirect":
    LOGGING_SCHEMA["loggers"] = get_logger_with_streaming_method(["file", "console"])
else:
    LOGGING_SCHEMA["loggers"] = get_logger_with_streaming_method(["file"])

dictConfig(LOGGING_SCHEMA)
