/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.ae.apponboarding.v2.exception;

/**
 * Error Message definition. These message will be returned to the external user in error or event responses
 *
 */
public final class ErrorMessages {

    // Error messages for ProblemDetails or OnboardingEvent 'Title' field
    public static final String CREATE_ONBOARDING_JOB_PROBLEM_TITLE = "Failed to create onboarding-job";
    public static final String PROCESS_CSAR_PROBLEM_TITLE = "Failed to process onboarding-job";
    public static final String DECOMPRESS_CSAR_PROBLEM_TITLE = "Failed to decompress CSAR";
    public static final String PARSER_PROBLEM_TITLE = "Failed to parse CSAR";

    // Error messages for ProblemDetails or OnboardingEvent 'Detail' field
    public static final String PLEASE_ONBOARD_VALID_CSAR = "Please onboard a valid csar archive file.";

    public static final String SAVE_TO_FILESYSTEM_ERROR = "IOException caught when saving onboarded %s to filesystem. Reason: %s";
    public static final String DATABASE_ACCESS_ERROR = "Failed to access the onboarding database. Reason: %s";
    public static final String DELETE_FROM_FILESYSTEM_ERROR = "IOException caught when deleting %s from filesystem. Reason: %s";
    public static final String DATA_ACCESS_EXCEPTION_DETAIL = "Data access error during communication to database. Reason: %s";
    public static final String TRANSACTION_EXCEPTION_DETAIL = "Transaction error during database access. Reason: %s";
    public static final String DELETE_JOB_INVALID_STATE = "onboarding-job status is not valid for deletion, current status is %s";

    //Error messages for GetByID Failure
    public static final String ONBOARDING_JOB_ENTITY_NOT_FOUND = "Onboarding-job with ID %s was not found in the DB";
    public static final String QUERY_MULTIPLE_PROPERTIES_NOT_ALLOWED = "Query with multiple onboarding-job properties is not supported.";

    //Error messages for Decompress Failures
    public static final String DECOMPRESS_CSAR_ERROR = "IOException caught when decompressing package %s to filesystem. Reason: %s";
    public static final String DECOMPRESS_CSAR_NOT_AVAILABLE_IN_TMP_ERROR = "CSAR package %s not available in %s";
    public static final String DECOMPRESS_CSAR_EXTRACT_ERROR = "Unable to decompress csar package %s. Reason: %s";

    //Error messages for Parser Failures
    public static final String PARSE_CSAR_ERROR = "IOException caught when parsing package %s from filesystem. Reason: %s";
    public static final String PARSE_YAML_ERROR = "Yaml file: %s parsing error. Invalid YAML format.";
    public static final String PARSE_APP_PERMISSIONS_EMPTY_SCOPE_ERROR = "Scope must be given for the resource: %s";
    public static final String PARSE_APP_DESCRIPTOR_YAML_IN_CSAR_ERROR = "App descriptor yaml file(s) in %s is not in correct format. Reason: %s";
    public static final String NONE_SUPPORTED_ARTIFACT_PARSER_ERROR = "App Component type, %s is not supported at the moment";
    public static final String UNKNOWN_APPCOMPONENT_DEFINITION_ERROR = "Unknown App Component definition in the App-Descriptor";
    public static final String PARSER_MANDATORY_FIELD_ERROR = "%s is required in %s";
    public static final String PARSER_FILE_NOT_EXIST_ERROR = "%s file doesn't exist in the csar %s";
    public static final String ARTIFACT_FILE_PATH_NOT_EXIST_ERROR = "Artifact file path is not found in %s";
    public static final String ASD_ARTIFACT_FILES_READ_ERROR = "Unable to read ASD artifact files from %s. Reason: %s";
    public static final String OPAQUE_ARTIFACT_FILES_READ_ERROR = "Unable to read %s artifact files from %s. Reason: %s";
    public static final String FAILED_TO_STORE_ARTIFACT_FAILURE_DETAIL = "Failed to upload %s artifact %s to object storage";
    public static final String FAILED_TO_STORE_ARTIFACTS_TITLE = "Failed to store artifacts";
    public static final String FAILED_TO_CREATE_BUCKET_DETAIL = "Failed to create storage location for artifacts in object storage";
    public static final String BUCKET_ALREADY_EXISTS_DETAIL = "Internal generated onboarding job ID is already in use. Please try again.";
    public static final String FAILED_TO_DELETE_ARTIFACTS_DETAIL ="Failed to delete artifacts from object storage";
    public static final String DELETE_BUCKET_STORE_ARTIFACT_FAILURE_TITLE = "Failed to remove artifacts";
    public static final String BUCKET_EXISTS_FAILURE_TITLE = "Request to object storage failed";
    public static final String BUCKET_EXISTS_FAILURE_DETAIL = "Failed during pre-check on object storage";
    public static final String ONBOARDING_JOB_ROLLBACK_TITLE = "Onboarding job rollback";
    public static final String ROLLBACK_REMOVED_ALL_ARTIFACTS_DETAIL = "Rollback completed. All artifacts stored have been removed.";
    public static final String ROLLBACK_FAILED_TO_DELETE_ARTIFACTS_DETAIL = "Rollback failed. Failed to remove all artifacts stored.";
    public static final String DUPLICATE_APP_EXISTS_DETAIL_APP_LCM = "App with same name, same version and same or no provider already exists";

    public static final String DUPLICATE_APP_EXISTS_DETAIL_APP_ONBOARDING = "App with same name and version already exists in App Onboarding";
    public static final String ERROR_ACCESSING_APP_LCM = "Exception while calling App LCM";
    public static final String LCM_REQUEST_FAILED_WITH_RETRIES_DETAIL = "Failed due to response status %s from App LCM";
    public static final String LCM_RESPONSE_MAPPING_FAILURE_DETAIL = "Failed to parse response body returned from App LCM";
    public static final String DESCRIPTOR_APPCOMPONENT_OR_APPCOMPONENT_LIST = "APPComponent or AppComponentList";


    private ErrorMessages() {
    }
}