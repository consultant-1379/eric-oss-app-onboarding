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

package com.ericsson.oss.ae.apponboarding.v2.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.consts.ParserConstants;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Permission;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Role;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.CreateAppRequest;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.service.parser.ArtifactParser;
import com.ericsson.oss.ae.apponboarding.v2.service.parser.ArtifactParserFactory;

/**
 * Parses all artifact content from the decompressed csar and creates an internal model.
 */
@Service
public class CsarParserService {

    @Autowired private ArtifactParserFactory artifactParserFactory;

    @Value("${onboarding-job.tempFolderLocation}") private String tempFolderLocation;

    private static final Logger logger = LoggerFactory.getLogger(CsarParserService.class);

    /**
     * Parse the content for all APP Components in the csar. Since different APP Component types store their
     * artifacts in their own way, a specific parser service instance will be called to handle each type.
     */
    public CreateAppRequest parseCsar(final OnboardingJobEntity onboardingJobEntity) {
        final UUID jobId = onboardingJobEntity.getId();
        final String csarName = onboardingJobEntity.getFileName();
        CreateAppRequest appRequest = null;
        logger.info("parseCsar() parsing csar package {} for onboarding-job {}", csarName, jobId);

        try {
            final Path jobFolderPath = Path.of(tempFolderLocation, jobId.toString());

            final Map<String, Object> toscaYamlFile = parseToscaMetaFile(jobFolderPath, csarName, onboardingJobEntity);
            final String appDescPath = extractAppDescriptorPath(toscaYamlFile);
            final Map<String, Object> appDescriptorYaml = parseAppDescriptor(jobFolderPath, appDescPath, csarName);

            appRequest = createAppRequest(appDescPath, appDescriptorYaml, onboardingJobEntity);

            // Parse all the component data for the create app request
            parseAllComponents(appDescriptorYaml, appDescPath, appRequest, jobId, csarName);

            logger.info("parseCsar() Parsing csar complete");
        } catch (final IOException e) {
            logger.error("parseCsar() error parsing csar file {}. Reason: {}", csarName, e.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE,
                    String.format(ErrorMessages.PARSE_CSAR_ERROR, csarName, e.getMessage()));
        } catch (final ClassCastException e) {
            final String errMessage = String.format(ErrorMessages.PARSE_APP_DESCRIPTOR_YAML_IN_CSAR_ERROR, csarName, e.getMessage());
            logger.error("parse() {}", errMessage);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMessage);
        }

        return appRequest;
    }

    private Map<String, Object> parseToscaMetaFile(final Path jobFolderPath, final String csarName, final OnboardingJobEntity onboardingJobEntity) throws IOException {
        try {
            final Path toscaFileRelPath = Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
            final Path toscaFilePath = jobFolderPath.resolve(toscaFileRelPath);
            ParserHelper.checkIfFileExist(toscaFilePath, toscaFileRelPath.toString(), csarName);

            final Map<String, Object> toscaYaml = ParserHelper.parseYamlFile(toscaFilePath);
            extractAndSetVendor(toscaYaml, onboardingJobEntity);

            return toscaYaml;
        } catch (ClassCastException e) {
            String errMessage = String.format(ErrorMessages.PARSE_YAML_ERROR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
            logger.error("parseToscaMetaFile() {}. Reason: {}", errMessage, e.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMessage);
        }
    }

    private void extractAndSetVendor(final Map<String, Object> toscaYamlFile, final OnboardingJobEntity onboardingJobEntity) {
        final String vendor = (String) toscaYamlFile.get(ParserConstants.TOSCA_CREATED_BY);
        ParserHelper.checkIsEmpty(vendor, ParserConstants.TOSCA_CREATED_BY, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        onboardingJobEntity.setVendor(vendor);
    }

    private String extractAppDescriptorPath(final Map<String, Object> toscaYamlFile) {
        return (String) toscaYamlFile.get(ParserConstants.TOSCA_ENTRY_DEFINITIONS);
    }

    private Map<String, Object> parseAppDescriptor(final Path jobFolderPath, final String appDescPath, final String csarName) throws IOException {
        ParserHelper.checkIsEmpty(appDescPath, ParserConstants.TOSCA_ENTRY_DEFINITIONS, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        final Path appDescriptorFilePath = jobFolderPath.resolve(appDescPath);
        ParserHelper.checkIfFileExist(appDescriptorFilePath, appDescPath, csarName);
        return ParserHelper.parseYamlFile(appDescriptorFilePath);
    }

    private CreateAppRequest createAppRequest(final String appDescPath, final Map<String, Object> appDescriptorYaml,
                                              final OnboardingJobEntity onboardingJobEntity) {
        final CreateAppRequest appRequest = new CreateAppRequest();
        extractAndSetAppData(appDescPath, appDescriptorYaml, appRequest, onboardingJobEntity);

        final List<Permission> permissions = extractPermissions(appDescriptorYaml);
        appRequest.setPermissions(permissions);

        final List<Role> roles = extractRoles(appDescriptorYaml);
        appRequest.setRoles(roles);

        return appRequest;
    }

    private void extractAndSetAppData(final String appDescPath, final Map<String, Object> appDescriptorYaml, final CreateAppRequest appRequest,
                                      final OnboardingJobEntity onboardingJobEntity) {
        final Map<String, Object> appDescription = (Map<String, Object>) appDescriptorYaml.get(ParserConstants.DESCRIPTOR_DESCRIPTION_OF_AN_APP);
        final String appName = (String) appDescription.get(ParserConstants.DESCRIPTOR_APP_NAME);
        ParserHelper.checkIsEmpty(appName, ParserConstants.DESCRIPTOR_APP_NAME, appDescPath);
        onboardingJobEntity.setAppName(appName);
        appRequest.setName(appName);

        final String appVersion = (String) appDescription.get(ParserConstants.DESCRIPTOR_APP_VERSION);
        ParserHelper.checkIsEmpty(appVersion, ParserConstants.DESCRIPTOR_APP_VERSION, appDescPath);
        onboardingJobEntity.setPackageVersion(appVersion);
        appRequest.setVersion(appVersion);

        final String appType = (String) appDescription.get(ParserConstants.DESCRIPTOR_APP_TYPE);
        ParserHelper.checkIsEmpty(appType, ParserConstants.DESCRIPTOR_APP_TYPE, appDescPath);
        onboardingJobEntity.setType(appType);
        appRequest.setType(appType);

        String appProvider = (String) appDescription.get(ParserConstants.DESCRIPTOR_APP_PROVIDER);
        appProvider = ParserHelper.checkIsEmptyOrNull(appProvider) ? onboardingJobEntity.getVendor() : appProvider;
        onboardingJobEntity.setProvider(appProvider);
        appRequest.setProvider(appProvider);
    }

    private List<Permission> extractPermissions(final Map<String, Object> appDescriptorYamlFile) {
        final List<Permission> permissions = new ArrayList<>();
        final List<Map<String, Object>> appPermissionsYamlList = (List<Map<String, Object>>) appDescriptorYamlFile.get(
                ParserConstants.DESCRIPTOR_APP_PERMISSIONS);

        if (appPermissionsYamlList != null && !appPermissionsYamlList.isEmpty()) {

            for (Map<String, Object> appPermissionYaml : appPermissionsYamlList) {
                if (appPermissionYaml != null && !appPermissionYaml.isEmpty()) {
                    permissions.add(getPermission(appPermissionYaml));
                }
            }
        }

        return permissions;
    }

    private Permission getPermission(final Map<String, Object> appPermissionYaml) {
        Map<String, Object> permissions = new HashMap<>();
        for (Map.Entry<String, Object> entry : appPermissionYaml.entrySet()) {
            permissions.put(entry.getKey().toUpperCase(Locale.getDefault()), entry.getValue());
        }

        String resourceName = (String) permissions.get(ParserConstants.DESCRIPTOR_APP_PERMISSIONS_RESOURCE);
        String scope = (String) permissions.get(ParserConstants.DESCRIPTOR_APP_PERMISSIONS_SCOPE);

        if (resourceName.equalsIgnoreCase(ParserConstants.APP_PERMISSIONS_RESOURCE_NAME_BDR) && StringUtils.isEmpty(scope)) {
            throw new OnboardingJobException(HttpStatus.BAD_REQUEST, ErrorMessages.PARSER_PROBLEM_TITLE,
                String.format(ErrorMessages.PARSE_APP_PERMISSIONS_EMPTY_SCOPE_ERROR, resourceName));
        }

        return Permission.builder().resource(resourceName).scope(scope).build();
    }

    private List<Role> extractRoles(final Map<String, Object> appDescriptorYamlFile) {
        final List<Role> roles = new ArrayList<>();
        final List<Map<String, String>> appRolesYamlList = (List<Map<String, String>>) appDescriptorYamlFile.get(ParserConstants.DESCRIPTOR_APP_ROLES);

        if (appRolesYamlList != null && !appRolesYamlList.isEmpty()) {

            for (Map<String, String> appRoleYaml : appRolesYamlList) {
                if (appRoleYaml != null && !appRoleYaml.isEmpty()) {
                    final String roleName = appRoleYaml.values().stream().findFirst().orElse(null);

                    if (StringUtils.isNoneBlank(roleName)) {
                        final Role appRole = Role.builder().name(roleName).build();
                        roles.add(appRole);
                    }
                }
            }
        }

        return roles;
    }

    private void parseAllComponents(final Map<String, Object> appDescriptorYamlFile, final String appDescPath, final CreateAppRequest appRequest,
                                    final UUID jobId, final String csarName) {

        ParserHelper.checkAppComponentKeysInYaml(appDescriptorYamlFile, ErrorMessages.DESCRIPTOR_APPCOMPONENT_OR_APPCOMPONENT_LIST, appDescPath);
        Object appComponentYamlData = null;
        if (appDescriptorYamlFile.containsKey(ParserConstants.DESCRIPTOR_APPCOMPONENT_LIST)) {
            appComponentYamlData = appDescriptorYamlFile.get(ParserConstants.DESCRIPTOR_APPCOMPONENT_LIST);
        }
        else {
            appComponentYamlData = appDescriptorYamlFile.get(ParserConstants.DESCRIPTOR_APPCOMPONENT);
        }
        ParserHelper.checkIsEmpty(appComponentYamlData, ErrorMessages.DESCRIPTOR_APPCOMPONENT_OR_APPCOMPONENT_LIST, appDescPath);
        if (appComponentYamlData instanceof List<?>) {
            final List<Map<String, Object>> appComponentYamlList = (List<Map<String, Object>>) appComponentYamlData;
            logger.info("parseAllComponents() list of {} Component(s) defined in App Descriptor file {}", appComponentYamlList.size(), appDescPath);
            for (Map<String, Object> appComponentYaml : appComponentYamlList) {
                parseComponent(appComponentYaml, appDescPath, appRequest, jobId, csarName);
            }
        } else if (appComponentYamlData instanceof Map<?, ?>) {
            final Map<String, Object> appComponentYaml = (Map<String, Object>) appComponentYamlData;
            parseComponent(appComponentYaml, appDescPath, appRequest, jobId, csarName);
        } else {
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE,
                ErrorMessages.UNKNOWN_APPCOMPONENT_DEFINITION_ERROR);
        }
    }

    private void parseComponent(final Map<String, Object> appComponentYaml, final String appDescPath, final CreateAppRequest appRequest,
                                final UUID jobId, final String csarName) {
        final Component component = createComponent(appComponentYaml, appDescPath);
        final String artifactType = component.getType();
        logger.info("parseComponent() Parsing artifacts for Component name={}, and type={}", component.getName(), artifactType);

        final String componentArtifactFilePath = extractAppComponentArtifactFilePath(appComponentYaml, appDescPath);

        final ArtifactParser artifactParser = artifactParserFactory.getParser(artifactType);
        artifactParser.parse(jobId, csarName, componentArtifactFilePath, component);
        appRequest.getComponents().add(component);
    }

    private Component createComponent(final Map<String, Object> appComponentYaml, final String appDescPath) {
        final Component component = new Component();
        final String artifactType = (String) appComponentYaml.get(ParserConstants.DESCRIPTOR_APPCOMPONENT_ARTIFACTTYPE);
        ParserHelper.checkIsEmpty(artifactType, ParserConstants.DESCRIPTOR_APPCOMPONENT_ARTIFACTTYPE, appDescPath);
        component.setType(artifactType);

        final String appComponentName = (String) appComponentYaml.get(ParserConstants.APP_COMPONENT_NAME_OF_COMPONENT);
        ParserHelper.checkIsEmpty(appComponentName, ParserConstants.APP_COMPONENT_NAME_OF_COMPONENT, appDescPath);
        component.setName(appComponentName);

        final String appComponentVersion = (String) appComponentYaml.get(ParserConstants.APP_COMPONENT_VERSION);
        ParserHelper.checkIsEmpty(appComponentVersion, ParserConstants.APP_COMPONENT_VERSION, appDescPath);
        component.setVersion(appComponentVersion);

        return component;
    }

    private String extractAppComponentArtifactFilePath(final Map<String, Object> appComponentYaml, final String appDescPath) {
        final String componentArtifactFilePath = (String) appComponentYaml.get(ParserConstants.DESCRIPTOR_APPCOMPONENT_ARTIFACT_FILE_PATH);
        ParserHelper.checkIsEmptyWithMessage(componentArtifactFilePath, ErrorMessages.ARTIFACT_FILE_PATH_NOT_EXIST_ERROR, appDescPath);
        return componentArtifactFilePath;
    }
}
