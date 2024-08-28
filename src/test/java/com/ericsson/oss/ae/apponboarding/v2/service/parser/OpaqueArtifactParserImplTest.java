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

import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.OPAQUE_FILE_PATH;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.REPLACEMENT_TEXT;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_COMPONENT_DATA_PATH;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_MULTPLE_COMPONENT_CSAR_NAME;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.ArtifactType;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils;

@SpringBootTest(classes = { CoreApplication.class, OpaqueArtifactParserImpl.class })
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class OpaqueArtifactParserImplTest {
    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;
    @Autowired
    private OpaqueArtifactParserImpl commonArtifactParser;

    private static final UUID JOB_ID = UUID.randomUUID();
    private static String tmpDirPath;

    @BeforeEach
    public final void init() throws IOException, URISyntaxException {
        final Path basePath = Path.of(tempFolderLocation, JOB_ID.toString());
        tmpDirPath = tempFolderLocation;
    }

    @AfterEach
    public final void restoreOpaqueFile() throws IOException {
        FileUtils.revertRenamedFile(tempFolderLocation, JOB_ID, Path.of(OPAQUE_FILE_PATH));
    }

    @AfterAll
    public static void wipeOutTempFolder() throws IOException {
        FileUtils.cleanUpFilesCreated(tmpDirPath, JOB_ID);
    }

    @Test
    public void testParse_success() throws IOException {
        final Path componentPath = Path.of(tempFolderLocation, JOB_ID.toString(), TEST_COMPONENT_DATA_PATH);
        FileUtils.createNewFileInFolder(componentPath , REPLACEMENT_TEXT);
        final Component component = new Component();

        Assertions.assertDoesNotThrow(() -> {
            commonArtifactParser.parse(JOB_ID, TEST_MULTPLE_COMPONENT_CSAR_NAME, OPAQUE_FILE_PATH, component);
        });

        Assertions.assertNotNull(component.getArtifacts());
        Assertions.assertEquals(1, component.getArtifacts().size());
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getType().equals(ArtifactType.OPAQUE.name())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getName().equals(componentPath .getFileName().toString())));
        Assertions
                .assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getName().equals(Path.of(OPAQUE_FILE_PATH).getFileName().toString())));
        Assertions.assertTrue(component.getArtifacts().stream().anyMatch(a -> a.getLocation().equals(Path.of(OPAQUE_FILE_PATH).toString())));
    }

    @Test
    public void testParse_failed_componentArtifactMissing() throws IOException {
        final Path componentPath = Path.of(tempFolderLocation, JOB_ID.toString(), TEST_COMPONENT_DATA_PATH);
        FileUtils.createNewFileInFolder(componentPath , REPLACEMENT_TEXT);
        FileUtils.renameFileInJobDir(tempFolderLocation, JOB_ID, Path.of(OPAQUE_FILE_PATH));
        final Component component = new Component();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            commonArtifactParser.parse(JOB_ID, TEST_MULTPLE_COMPONENT_CSAR_NAME, OPAQUE_FILE_PATH, component);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_FILE_NOT_EXIST_ERROR, OPAQUE_FILE_PATH, TEST_MULTPLE_COMPONENT_CSAR_NAME);
    }

    @Test
    public void testParse_failed_invalidComponentType() throws IOException {
        boolean response = commonArtifactParser.isSupported("COMPONENT_INVALID");
        Assertions.assertFalse(response);
    }

    @Test
    public void testParse_failed_IOException() throws IOException {
        final Path componentPath = Path.of(tempFolderLocation, JOB_ID.toString(), TEST_COMPONENT_DATA_PATH);
        FileUtils.createNewFileInFolder(componentPath , REPLACEMENT_TEXT);
        String ioExceptionMessage = "File read error";
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.walk(Mockito.any(Path.class))).thenThrow(new IOException(ioExceptionMessage));
            final Component component = new Component();

            final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
                commonArtifactParser.parse(JOB_ID, TEST_MULTPLE_COMPONENT_CSAR_NAME, OPAQUE_FILE_PATH, component);
            });

            TestUtils.assertInternalServerError(exception, ioExceptionMessage);
        }

    }


}
