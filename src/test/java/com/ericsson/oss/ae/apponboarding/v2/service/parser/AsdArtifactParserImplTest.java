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

import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.v2.consts.ParserConstants;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.ArtifactType;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils;

@SpringBootTest(classes = { CoreApplication.class, AsdArtifactParserImpl.class })
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class AsdArtifactParserImplTest {
    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;
    @Autowired
    private AsdArtifactParserImpl asdArtifactParser;

    private static final UUID JOB_ID = UUID.randomUUID();
    private static String tmpDirPath;

    @BeforeEach
    public final void init() throws IOException, URISyntaxException {
        final Path basePath = Path.of(tempFolderLocation, JOB_ID.toString());
        final Path asdFileAbsolutePath = basePath.resolve(ASD_FILE_PATH);
        FileUtils.copyFileToFolder(asdFileAbsolutePath.getParent(), asdFileAbsolutePath.getFileName().toString(), TEST_ASD_FILE_LOCATION);
        final Path helmChartAbsPath = basePath.resolve(TEST_HELM_CHART_PATH);
        FileUtils.createNewFileInFolder(helmChartAbsPath, REPLACEMENT_TEXT);
        tmpDirPath = tempFolderLocation;
    }

    @AfterEach
    public final void restoreAsdFile() throws IOException {
        FileUtils.revertRenamedFile(tempFolderLocation, JOB_ID, Path.of(ASD_FILE_PATH));
    }

    @AfterAll
    public static void wipeOutTempFolder() throws IOException {
        FileUtils.cleanUpFilesCreated(tmpDirPath, JOB_ID);
    }

    @Test
    public void testParse_success() throws IOException {
        final Path dockerImagePath = Path.of(tempFolderLocation, JOB_ID.toString(), TEST_DOCKER_IMAGE_PATH);
        FileUtils.createNewFileInFolder(dockerImagePath, REPLACEMENT_TEXT);
        FileUtils.createNewFileInFolder(dockerImagePath.getParent().resolve(TEST_DUMMY_FILENAME), REPLACEMENT_TEXT);
        final Component component = new Component();

        Assertions.assertDoesNotThrow(() -> {
            asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
        });

        Assertions.assertNotNull(component.getArtifacts());
        Assertions.assertEquals(4, component.getArtifacts().size());
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getType().equals(ArtifactType.IMAGE.name())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getType().equals(ArtifactType.HELM.name())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getType().equals(ArtifactType.OPAQUE.name())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getName().equals(dockerImagePath.getFileName().toString())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getName().equals(TEST_ASD_APPLICATION_NAME)));
        Assertions
                .assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getName().equals(Path.of(ASD_FILE_PATH).getFileName().toString())));
        Assertions.assertTrue(
                component.getArtifacts().stream().anyMatch(a -> a.getName().equals(Path.of(TEST_DUMMY_FILENAME).getFileName().toString())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getLocation().equals(Path.of(TEST_DOCKER_IMAGE_PATH).toString())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getLocation().equals(Path.of(TEST_HELM_CHART_PATH).toString())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getLocation().equals(Path.of(ASD_FILE_PATH).toString())));
        Assertions.assertTrue(component.getArtifacts().stream()
                .anyMatch(a -> a.getLocation().equals(Path.of(TEST_DOCKER_IMAGE_PATH).getParent().resolve(TEST_DUMMY_FILENAME).toString())));
    }

    @Test
    public void testParse_failed_asdFileNotExist() throws IOException {
        FileUtils.renameFileInJobDir(tempFolderLocation, JOB_ID, Path.of(ASD_FILE_PATH));
        final Component component = new Component();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_FILE_NOT_EXIST_ERROR, ASD_FILE_PATH, TEST_CSAR_NAME);
    }

    @Test
    public void testParse_failed_asdFileNotReadable() throws IOException {
        FileUtils.replaceFileWithContentInJobDir(tempFolderLocation, JOB_ID, Path.of(ASD_FILE_PATH), MALFORMED_BYTES);
        final Component component = new Component();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_CSAR_ERROR, TEST_CSAR_NAME, "");
    }

    @Test
    public void testParse_failed_asdFileWithNoAsdApplicationName() throws IOException {
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, JOB_ID, Path.of(ASD_FILE_PATH), ParserConstants.ASD_APPLICATION_NAME);
        final Component component = new Component();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.ASD_APPLICATION_NAME,
                ASD_FILE_PATH);
    }

    @Test
    public void testParse_failed_asdFileWithNoDeploymentItems() throws IOException {
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, JOB_ID, Path.of(ASD_FILE_PATH), ParserConstants.ASD_DEPLOYMENT_ITEMS);
        final Component component = new Component();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.ASD_DEPLOYMENT_ITEMS,
                ASD_FILE_PATH);
    }

    @Test
    public void testParse_failed_asdFileWithInvalidDeploymentItems() throws IOException {
        FileUtils.replaceYamlArrayInFileInJobDir(tempFolderLocation, JOB_ID, ASD_FILE_PATH, ParserConstants.ASD_DEPLOYMENT_ITEMS);
        final Component component = new Component();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_YAML_ERROR, ASD_FILE_PATH);
    }

    @Test
    public void testParse_failed_asdFileWithNoArtifactId() throws IOException {
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, JOB_ID, Path.of(ASD_FILE_PATH), ParserConstants.ASD_DEPLOYMENT_ARTIFACT_ID);
        final Component component = new Component();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.ASD_DEPLOYMENT_ARTIFACT_ID,
                ASD_FILE_PATH);
    }

    @Test
    public void testParse_failed_IOException() throws IOException {
        final Path dockerImagePath = Path.of(tempFolderLocation, JOB_ID.toString(), TEST_DOCKER_IMAGE_PATH);
        FileUtils.createNewFileInFolder(dockerImagePath, REPLACEMENT_TEXT);
        FileUtils.createNewFileInFolder(dockerImagePath.getParent().resolve(TEST_DUMMY_FILENAME), REPLACEMENT_TEXT);
        final Component component = new Component();
        String ioExceptionMessage = "File read error";
        String yamlContents = Files.readString(Path.of(tempFolderLocation, JOB_ID.toString(),ASD_FILE_PATH));
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(Mockito.any(Path.class))).thenReturn(yamlContents);
            mockedFiles.when(() -> Files.walk(Mockito.any(Path.class))).thenThrow(new IOException(ioExceptionMessage));

            final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
                asdArtifactParser.parse(JOB_ID, TEST_CSAR_NAME, ASD_FILE_PATH, component);
            });

            TestUtils.assertInternalServerError(exception, ioExceptionMessage);
        }
    }
}
