/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.ae.apponboarding.common.consts;

public class Consts {

    private Consts() {
    }

    public static final String TEMP_FILE_ATT_NAME = "TEMP_FILE";
    public static final String API_V1_APPS = "/v1/apps";
    public static final String API_ARTIFACT = "artifacts";
    public static final String FILE = "file";
    public static final String QUERY_FETCH_ALL_APPS = "SELECT DISTINCT app FROM Application app "
            + "LEFT JOIN app.events e LEFT JOIN app.artifacts a ORDER BY app.id";
    public static final String API = "/api/";
    public static final String UNDERSCORE = "_";
    public static final String CHARTS = "/charts/";

    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String EQUAL = "=";
    public static final String SPACE = " ";
    public static final String FORWARD_SLASH = "/";
    public static final String QUERY_PARAM_KEY_VALUE_SEPARATOR = ":";
    public static final int DEFAULT_OFFSET = 0;
    public static final String QUERY_PARAM_ERR_ID = "queryParam";
    public static final String QUERY_PARAM_INVALID_FORMAT_ERR_CODE = "validation.queryParam.invalid.format";
    public static final String QUERY_PARAM_INVALID_FORMAT_ERR_MSG = "Query Parameter should be a key-value pair separated by colon";
    public static final String QUERY_PARAM_EMPTY_VALUE_ERR_CODE = "validation.queryParam.value.empty";
    public static final String QUERY_PARAM_EMPTY_VALUE_ERR_MSG = "Query Parameter value can't be empty";
    public static final String QUERY_PARAM_UNKNOWN_FIELD_ERR_CODE = "validation.queryParam.unknown.field";
    public static final String QUERY_PARAM_UNKNOWN_FIELD_ERR_MSG = "Query Parameter key should be either name, vendor, version or id";
    public static final String QUERY_PARAM_INVALID_ID_FORMAT_ERR_CODE = "validation.queryParam.id.invalid.format";
    public static final String QUERY_PARAM_INVALID_ID_FORMAT_ERR_MSG = "Query Parameter ID should be a valid number";
    public static final String QUERY_PARAM_INVALID_ID_FORMAT_V2_ERR_MSG = "Query Parameter ID should be a valid UUID";
    public static final String SORT_ERR_ID = "sort";
    public static final String SORT_ERR_CODE = "validation.sort.unknown.field";
    public static final String SORT_ERR_MSG = "Sort key should be either name, vendor, version or id";
    public static final String SORT_V2_ERR_MSG = "Sort key should be either fileName, vendor, packageVersion, id, type, packageSize, status or appId";
    public static final String QUERY_PARAM_INVALID_STATUS_ERR_CODE = "validation.queryParam.status.invalid.format";
    public static final String QUERY_PARAM_INVALID_STATUS_ERR_MSG = "Status should be either UPLOADED, UNPACKED, PARSED, ONBOARDED, FAILED or ROLLBACK_FAILED";
    public static final String PAGE_LIMIT_ERR_ID = "limit";
    public static final String PAGE_LIMIT_ERR_CODE = "validation.limit.invalid.number";
    public static final String PAGE_LIMIT_ERR_MSG = "Limit should be a valid number";
    public static final String OFFSET_ERR_ID = "offset";
    public static final String OFFSET_ERR_CODE = "validation.offset.invalid.number";
    public static final String OFFSET_ERR_MSG = "Offset should be a valid number";
    public static final String OFFSET_LIMIT_ERR_ID = "offSetLimit";
    public static final String OFFSET_LIMIT_ERR_CODE = "validation.offset.require.limit";
    public static final String OFFSET_LIMIT_ERR_MSG = "Offset requires a valid limit";
    public static final String APP_ONBOARDING_V2 = "/v2";
    public static final String ONBOARDING_JOBS_V2_API = APP_ONBOARDING_V2 + "/onboarding-jobs";
    public static final String APP_PACKAGES_V2_API = APP_ONBOARDING_V2 + "/app-packages";
    public static final String INVALID_PATH_ONBOARDING_JOBS_API = "onboardingjobs";
    public static final String ONBOARDING_DB_V2_SCHEMA = "acm_schema";
    public static final String ONBOARDING_EVENT_COLUMN_ONBOARDING_JOB_ID = "onboarding_job_id";
    public static final String QUERY_FETCH_ALL_ONBOARDING_JOBS = "SELECT DISTINCT job FROM OnboardingJobEntity job "
            + "LEFT JOIN job.onboardingEventEntities event ORDER BY job.startTimestamp";
    public static final int ENTITY_FIELD_LENGTH_255 = 255;
    public static final int ENTITY_FIELD_LENGTH_50 = 50;
    public static final int ENTITY_FIELD_LENGTH_36 = 36;
    public static final int ONBOARDED_TIME_DIFFERENCE = 1000 * 60 * 60;
    public static final int ONE_MIB_IN_BYTES = 1024 * 1024;
    public static final int ONE_MB_IN_BYTES = 1000 * 1000;
    public static final String APP_PKG_PROCESSOR_EXECUTOR_THREAD_POOL_NAME = "app-onboarding-package-executor";
    public static final String MEBIBYTE_SYMBOL = "MiB";
    public static final String MEGABYTE_SYMBOL = "MB";
    public static final String PUSH_OBJECT_STORE_SUCCESS_TITLE = "Stored %s out of %s artifacts";
    public static final String PUSH_OBJECT_STORE_SUCCESS_DETAIL = "Uploaded %s";
    public static final String FILE_NAME_ERR_ID = "fileName";
    public static final String FILE_NAME_ERR_CODE = "validation.invalid.filename";
    public static final String FILE_NAME_INVALID = "File name contains invalid characters, %s";
    public static final String FILE_TYPE_ERR_ID = "fileType";
    public static final String FILE_TYPE_ERR_CODE = "validation.invalid.filetype";
    public static final String FILE_TYPE_INVALID = "File type is invalid, %s";
    public static final String FILE_NAME_UNSAFE_CSAR = "../temp/hello.csar";
    public static final String FILE_TYPE_INVALID_CSAR = "hello.txt";
    public static final String FILE_TYPE_AND_NAME_INVALID_CSAR = "../tmp/hello.txt";
    public static final String FILE_NAME_VALID_CSAR = "hello.csar";
    public static final String PARENT_DIR = "..";
    public static final String DIR_SEPARATOR_LINUX = "/";
    public static final String DIR_SEPARATOR_WINDOWS = "\\";
    public static final String CSAR_EXTENSION = ".csar";
    public static final String STRING_SEPARATOR = ". ";
    public static final String TRACING_ENDPOINT = "ericsson.tracing.exporter.endpoint";
    public static final String TRACING_JAEGER_ENDPOINT = "ericsson.tracing.sampler.jaeger-remote.endpoint";
    public static final String TRACING_ENDPOINT_GRPC = "http://eric-dst-collector:4317";
    public static final String TRACING_ENDPOINT_HTTP = "http://eric-dst-collector:4318";
    public static final String TRACING_JAEGER_ENDPOINT_GRPC = "http://eric-dst-collector:14250";
    public static final String TRACING_POLING = "ericsson.tracing.polingInterval";
    public static final String TRACING_POLING_INTERVAL = "30";
    public static final String GET_METHOD = "GET";
    public static final String SUBJECT_NOT_AVAILABLE = "n/a";
    public static final String APP_MODE_ENABLED = "ENABLED";
    public static final String APP_MODE_DISABLED = "DISABLED";
    public static final String APP_STATUS_DELETING = "DELETING";
}
