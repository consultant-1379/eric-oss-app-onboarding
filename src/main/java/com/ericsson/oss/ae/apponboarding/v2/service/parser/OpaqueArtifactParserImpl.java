/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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
package com.ericsson.oss.ae.apponboarding.v2.service.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.ericsson.oss.ae.apponboarding.v2.consts.ParserConstants;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.ParserHelper;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Artifact;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.ArtifactType;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;

/**
 * Parses OPAQUE artifacts for an APP Component types containing only OAPQUE artifacts and updates the internal model. In this parser it will read
 * the list of artifacts into an internal model, for an App Component of an uploaded CSAR.
 */
@Service
public class OpaqueArtifactParserImpl implements ArtifactParser {

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;
    private static final List<String> SUPPORTED_ARTIFACT_TYPES = List.of(ParserConstants.APP_COMPONENT_ARTIFACT_TYPE_DATA_MANAGEMENT);
    private static final Logger logger = LoggerFactory.getLogger(OpaqueArtifactParserImpl.class);

    @Override
    public boolean isSupported(final String type) {
        return SUPPORTED_ARTIFACT_TYPES.contains(type.toUpperCase(Locale.getDefault()));
    }

    @Override
    public void parse(final UUID jobId, final String csarName, final String artifactFilePath, final Component component) {
        logger.info("parse() parsing all OPAQUE artifacts for onboarding-job {}", jobId);
        final List<Artifact> artifacts = parseAllFilesInComponentFolder(jobId, csarName, artifactFilePath, component.getType());
        appendArtifactsToAppComponent(artifacts, component);
    }

    private List<Artifact> parseAllFilesInComponentFolder(final UUID jobId, final String csarName, final String artifactFilePath, final String type) {
        final Path baseDirPath = Path.of(tempFolderLocation, jobId.toString());
        final Path appComponentPath = getAppComponentFolder(baseDirPath, csarName, artifactFilePath);

        try (final Stream<Path> walk = Files.walk(appComponentPath)) {
            return walk.filter(path -> !Files.isDirectory(path))
            .map(path -> createArtifact(jobId, path)).collect(Collectors.toList());
        } catch (final IOException e) {
            final String errMessage = String.format(ErrorMessages.OPAQUE_ARTIFACT_FILES_READ_ERROR, type, artifactFilePath, e.getMessage());
            logger.error("parseAllFilesInComponentFolder() {}", errMessage);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMessage);
        }
    }

    private Path getAppComponentFolder(final Path baseDirPath, final String csarName, final String opaqueFilePath) {
        final Path componentPath = baseDirPath.resolve(opaqueFilePath);
        ParserHelper.checkIfFileExist(componentPath, opaqueFilePath, csarName);
        return componentPath;
    }

    private Artifact createArtifact(final UUID jobId, final Path filePath) {
        final Path baseDirPath = Path.of(tempFolderLocation, jobId.toString());
        final Path relativePathToFile = baseDirPath.relativize(filePath);

        return Artifact.builder()
            .name(filePath.getFileName().toString())
            .type(ArtifactType.OPAQUE.name())
            .location(relativePathToFile.toString())
            .build();
    }

    private void appendArtifactsToAppComponent(final List<Artifact> artifacts, final Component appComponent) {
        if (artifacts != null && !artifacts.isEmpty()) {
            List<Artifact> existingArtifacts = appComponent.getArtifacts();

            if (existingArtifacts == null) {
                existingArtifacts = new ArrayList<>();
            }

            existingArtifacts.addAll(artifacts);
            appComponent.setArtifacts(existingArtifacts);
        }
    }
}