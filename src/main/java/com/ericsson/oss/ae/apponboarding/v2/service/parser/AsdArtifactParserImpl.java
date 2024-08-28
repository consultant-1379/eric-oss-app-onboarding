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

package com.ericsson.oss.ae.apponboarding.v2.service.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Artifact;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.ArtifactType;
import com.ericsson.oss.ae.apponboarding.v2.service.ParserHelper;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;

/**
 * Parses Microservice/ASD artifacts for an APP Component that is Type=Microservice/ASD and updates the internal model. In this parser it will read
 * the list of artifacts into an internal model, for an App Component of an uploaded CSAR.
 */
@Service
public class AsdArtifactParserImpl implements ArtifactParser {

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;
    private static final List<String> SUPPORTED_ARTIFACT_TYPES = List.of(ParserConstants.APP_COMPONENT_ARTIFACT_TYPE_ASD,
            ParserConstants.APP_COMPONENT_ARTIFACT_TYPE_MICROSERVICE);
    private static final Logger logger = LoggerFactory.getLogger(AsdArtifactParserImpl.class);

    @Override
    public boolean isSupported(final String type) {
        return SUPPORTED_ARTIFACT_TYPES.contains(type.toUpperCase(Locale.getDefault()));
    }

    @Override
    public void parse(final UUID jobId, final String csarName, final String asdFilePath, final Component component) {
        logger.info("parse() parsing all ASD artifacts for onboarding-job {}", jobId);

        try {
            final Map<String, Object> asdYaml = parseAsdArtifactFile(jobId, asdFilePath, csarName);
            final String helmChartLocation = extractHelmChartLocation(jobId, asdYaml, asdFilePath, csarName);
            final List<Artifact> artifacts = parseAllFilesInAsdFolder(jobId, csarName, asdFilePath, asdYaml, helmChartLocation);
            appendArtifactsToAppComponent(artifacts, component);

            logger.info("parse() ASD parsing for AppComponent {} is completed. Artifacts: {}", asdYaml.getOrDefault(ParserConstants.ASD_APPLICATION_NAME, null), artifacts);
        } catch (final IOException e) {
            logger.error("parse() error parsing ASD AppComponent. Reason: {}", e.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE,
                    String.format(ErrorMessages.PARSE_CSAR_ERROR, csarName, e.getMessage()));
        } catch (final ClassCastException e) {
            logger.error("parse() ASD yaml file is not in correct format. Reason: {}", e.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE,
                    String.format(ErrorMessages.PARSE_YAML_ERROR, asdFilePath));
        }
    }

    private Map<String, Object> parseAsdArtifactFile(final UUID jobId, final String asdFilePath, final String csarName) throws IOException {
        final Path asdYamlPath = Path.of(tempFolderLocation, jobId.toString(), asdFilePath);
        ParserHelper.checkIfFileExist(asdYamlPath, asdFilePath, csarName);
        return ParserHelper.parseYamlFile(asdYamlPath);
    }

    private String extractHelmChartLocation(final UUID jobId, final Map<String, Object> asdYaml, final String asdFilePath, final String csarName) {
        final Map<String, Object> deploymentItems = (Map<String, Object>) asdYaml.get(ParserConstants.ASD_DEPLOYMENT_ITEMS);
        ParserHelper.checkIsEmpty(deploymentItems, ParserConstants.ASD_DEPLOYMENT_ITEMS, asdFilePath);

        final String helmChartLocation = (String) deploymentItems.get(ParserConstants.ASD_DEPLOYMENT_ARTIFACT_ID);
        ParserHelper.checkIsEmpty(helmChartLocation, ParserConstants.ASD_DEPLOYMENT_ARTIFACT_ID, asdFilePath);
        ParserHelper.checkIfFileExist(Path.of(tempFolderLocation, jobId.toString(), helmChartLocation), helmChartLocation, csarName);

        return helmChartLocation;
    }

    private List<Artifact> parseAllFilesInAsdFolder(final UUID jobId, final String csarName, final String asdFilePath,
                                                    final Map<String, Object> asdYaml, final String helmChartRelativePath) {
        final Path baseDirPath = Path.of(tempFolderLocation, jobId.toString());
        final Path asdDirPath = getAsdFolder(baseDirPath, csarName, asdFilePath);

        try (final Stream<Path> walk = Files.walk(asdDirPath)) {
            return walk.filter(path -> !Files.isDirectory(path))
                    .map(path -> createArtifact(jobId, path, asdYaml, asdFilePath, Path.of(helmChartRelativePath))).collect(Collectors.toList());
        } catch (final IOException e) {
            final String errMessage = String.format(ErrorMessages.ASD_ARTIFACT_FILES_READ_ERROR, asdFilePath, e.getMessage());
            logger.error("parseAllFilesInAsdFolder() {}", errMessage);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMessage);
        }
    }

    private Path getAsdFolder(final Path baseDirPath, final String csarName, final String asdFilePath) {
        final Path asdDirPath = baseDirPath.resolve(asdFilePath).getParent();
        ParserHelper.checkIfFileExist(asdDirPath, asdFilePath, csarName);
        return asdDirPath;
    }

    private Artifact createArtifact(final UUID jobId, final Path filePath, final Map<String, Object> asdYaml, final String asdFilePath,
                                    final Path helmChartRelativePath) {
        final Path baseDirPath = Path.of(tempFolderLocation, jobId.toString());
        final Path relativePathToFile = baseDirPath.relativize(filePath);

        return Artifact.builder().name(getArtifactName(jobId, filePath, asdYaml, asdFilePath, helmChartRelativePath))
                .type(getArtifactType(jobId, filePath, helmChartRelativePath).name()).location(relativePathToFile.toString()).build();
    }

    private String getArtifactName(final UUID jobId, final Path filePath, final Map<String, Object> asdYaml, final String asdFilePath,
                                   final Path helmChartRelativePath) {
        final Path baseDirPath = Path.of(tempFolderLocation, jobId.toString());

        if (isHelmChart(baseDirPath, filePath, helmChartRelativePath)) {
            return extractHelmChartName(asdYaml, asdFilePath);
        } else {
            return filePath.getFileName().toString();
        }
    }

    private boolean isHelmChart(final Path baseDirPath, final Path filePath, final Path helmChartRelativePath) {
        final Path relativePathToFile = baseDirPath.relativize(filePath);
        return relativePathToFile.equals(helmChartRelativePath);
    }

    private String extractHelmChartName(final Map<String, Object> asdYaml, final String asdFilePath) {
        final String artifactName = (String) asdYaml.get(ParserConstants.ASD_APPLICATION_NAME);
        ParserHelper.checkIsEmpty(artifactName, ParserConstants.ASD_APPLICATION_NAME, asdFilePath);

        return artifactName;
    }

    private ArtifactType getArtifactType(final UUID jobId, final Path filePath, final Path helmChartRelativePath) {
        final Path baseDirPath = Path.of(tempFolderLocation, jobId.toString());

        if (isHelmChart(baseDirPath, filePath, helmChartRelativePath)) {
            return ArtifactType.HELM;
        } else if (isDockerImage(filePath)) {
            return ArtifactType.IMAGE;
        } else {
            return ArtifactType.OPAQUE;
        }
    }

    private boolean isDockerImage(final Path filePath) {
        final String dirPathOfFile = filePath.toString().toUpperCase(Locale.getDefault());
        final Path parentDirOfFile = Path.of(dirPathOfFile).getParent();
        return parentDirOfFile.equals(parentDirOfFile.getParent().resolve(ParserConstants.IMAGE_FOLDER_NAME))
                && dirPathOfFile.endsWith(ParserConstants.FILE_TYPE_TAR);
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
