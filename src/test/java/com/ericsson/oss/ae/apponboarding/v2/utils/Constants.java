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

package com.ericsson.oss.ae.apponboarding.v2.utils;

import java.util.UUID;

public class Constants {

    private Constants() {
    }

    //Constants shared across the tests
    public static UUID ONBOARDING_JOB_ID = UUID.fromString("9cc1047a-5aae-4630-893a-1536392cbd2b");
     public static String APP_PACKAGE_FILE_NAME = "eric-oss-hello-world-app.csar";
    public static String APP_PACKAGE_VENDOR_ERICSSON = "Ericsson";
    public static String APP_PACKAGE_TYPE_RAPP = "rApp";
    public static String APP_PACKAGE_USER_NAME = "sampleUser";
    public static String APP_PACKAGE_VERSION = "1.0.0-1";
    public static String APP_PACKAGE_VERSION_2 = "2.2.2";
    public static String APP_PACKAGE_SIZE = "100MiB";
    public static final String TEST_CSAR_NAME = "test.csar";
    public static final String TEST_CSAR_WITH_PROVIDER_NAME = "test_with_provider.csar";
    public static final String TEST_MULTPLE_COMPONENT_CSAR_NAME = "testMultipleComponent.csar";
    public static final String TEST_CSAR_APP_NAME = "App-Onboarding-helloWorld";
    public static final String TEST_CSAR_APP_VERSION = "1.0.0";
    public static final String TEST_CSAR_APP_VERSION_2 = "2.2.2";
    public static final String TEST_CSAR_APP_TYPE = "rApp";
    public static final String TEST_CSAR_APP_PROVIDER = "Ericsson";
    public static final String TEST_CSAR_APP_COMPONENT_TYPE = "Microservice";
    public static final String INVALID_ARCHIVE_CSAR_NAME = "invalid_archive.csar";
    public static final String TEST_CSAR_LOCATION = "csar/" + TEST_CSAR_NAME;
    public static final String TEST_CSAR_WITH_PROVIDER_LOCATION = "csar/" + TEST_CSAR_WITH_PROVIDER_NAME;
    public static final String TEST_APP_DESCRIPTOR_BOTH_COMPONENTS_NAME = "testInvalidBothComponentFields.csar";
    public static final String TEST_APP_DESCRIPTOR_BOTH_COMPONENTS_LOCATION = "csar/invalidCsars/testInvalidBothComponentFields.csar";
    public static final String TEST_CSAR_NO_COMPONENT_LOCATION = "csar/invalidCsars/testMultipleComponentNoComponent.csar";
    public static final String TEST_CSAR_NO_COMPONENT_NAME = "testMultipleComponentNoComponent.csar";
    public static final String TEST_CSAR_MULTIPLE_COMPONENT_INVALID_PATH_LOCATION = "csar/invalidCsars/testMultipleComponentInvalidPath.csar";
    public static final String TEST_CSAR_MULTIPLE_COMPONENT_INVALID_PATH_NAME = "testMultipleComponentInvalidPath.csar";
    public static final String TEST_CSAR_MULTIPLE_COMPONENT_INVALID_LIST_LOCATION = "csar/invalidCsars/testMultipleComponentInvalidList.csar";
    public static final String TEST_CSAR_MULTIPLE_COMPONENT_INVALID_LIST_NAME = "testMultipleComponentInvalidList.csar";
    public static final String TEST_MULTPLE_COMPONENT_CSAR_LOCATION = "csar/" + TEST_MULTPLE_COMPONENT_CSAR_NAME;
    public static final String INVALID_ARCHIVE_CSAR_LOCATION = "csar/invalidCsars/" + INVALID_ARCHIVE_CSAR_NAME;
    public static final String TEST_CSAR_INVALID_PATH_NAME = "#%&{}/\\";
    public static final String APP_DESCRIPTOR_LOCATION_IN_CSAR = "/Definitions/AppDescriptor.yaml";

    public static final String APP_DESCRIPTOR_LOCATION_IN_CSAR_WITHOUT_INITIAL_SLASH = "Definitions/AppDescriptor.yaml";
    public static final byte[] MALFORMED_BYTES = { (byte) 0xFF, (byte) 0xFE };
    public static final String TEST_ASD_FILE_LOCATION = "csar/ASD.yaml";
    public static final String REPLACEMENT_TEXT = "Hello";
    public static final String REPLACEMENT_FILENAME_SUFFIX = ".bak";
    public static final String ASD_FILE_PATH = "OtherDefinitions/ASD/ASD.yaml";
    public static final String OPAQUE_FILE_PATH = "OtherDefinitions/DataManagement/input-data-specification.json.txt";
    public static final String TEST_ASD_APPLICATION_NAME = "eric-oss-app-onboarding";
    public static final String TEST_HELM_CHART_PATH = "OtherDefinitions/ASD/eric-oss-app-onboarding-0.1.0-1.tgz";
    public static final String TEST_DOCKER_IMAGE_PATH = "OtherDefinitions/ASD/images/docker.tar";
    public static final String TEST_COMPONENT_DATA_PATH = "OtherDefinitions/DataManagement/input-data-specification.json.txt";
    public static final String TEST_DUMMY_FILENAME = "dummy.file";
    public static final String COLON_CHAR = ":";
    public static final String SPACE_CHAR = " ";
    public static final String NEW_LINE = "\n";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    public static final String SERVICE_UNAVAILABLE = "Service Unavailable";

    public static final String DATA_MANAGEMENT_ARTEFACT_TYPE = "ArtefactType: DataManagement";

    public static final String DATA_MANAGEMENT_INAVALID_ARTEFACT_TYPE = "ArtefactType: Invalid";
}
