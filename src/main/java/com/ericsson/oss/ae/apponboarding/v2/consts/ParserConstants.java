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

package com.ericsson.oss.ae.apponboarding.v2.consts;

public final class ParserConstants {
    public static final String METADATA_FOLDER_NAME_IN_CSAR = "Metadata";
    public static final String TOSCA_FILE_NAME_IN_CSAR = "Tosca.meta";
    public static final String TOSCA_ENTRY_DEFINITIONS = "Entry-Definitions";
    public static final String TOSCA_CREATED_BY = "Created-By";
    public static final String DESCRIPTOR_DESCRIPTION_OF_AN_APP = "Description of an APP";
    public static final String DESCRIPTOR_APP_NAME = "APPName";
    public static final String DESCRIPTOR_APP_VERSION = "APPVersion";
    public static final String DESCRIPTOR_APP_TYPE = "APPType";
    public static final String DESCRIPTOR_APP_PROVIDER = "APPProvider";
    public static final String DESCRIPTOR_APP_PERMISSIONS = "APPPermissions";
    public static final String DESCRIPTOR_APP_PERMISSIONS_RESOURCE = "RESOURCE";
    public static final String DESCRIPTOR_APP_PERMISSIONS_SCOPE = "SCOPE";
    public static final String APP_PERMISSIONS_RESOURCE_NAME_BDR = "BDR";
    public static final String DESCRIPTOR_APP_ROLES = "APPRoles";
    public static final String DESCRIPTOR_APPCOMPONENT = "APPComponent";
    public static final String DESCRIPTOR_APPCOMPONENT_LIST = "AppComponentList";
    public static final String DESCRIPTOR_APPCOMPONENT_ARTIFACT_FILE_PATH = "Path";
    public static final String DESCRIPTOR_APPCOMPONENT_ARTIFACTTYPE = "ArtefactType";
    public static final String APP_COMPONENT_NAME_OF_COMPONENT = "NameofComponent";
    public static final String APP_COMPONENT_VERSION = "Version";
    public static final String APP_COMPONENT_ARTIFACT_TYPE_ASD = "ASD";
    public static final String APP_COMPONENT_ARTIFACT_TYPE_MICROSERVICE = "MICROSERVICE";
    public static final String ASD_APPLICATION_NAME = "asdApplicationName";
    public static final String ASD_DEPLOYMENT_ITEMS  = "deploymentItems";
    public static final String ASD_DEPLOYMENT_ARTIFACT_ID  = "artifactId";
    public static final String IMAGE_FOLDER_NAME = "IMAGES";
    public static final String FILE_TYPE_TAR = ".TAR";
    public static final String APP_COMPONENT_ARTIFACT_TYPE_DATA_MANAGEMENT = "DATAMANAGEMENT";

    private ParserConstants() {}
}
