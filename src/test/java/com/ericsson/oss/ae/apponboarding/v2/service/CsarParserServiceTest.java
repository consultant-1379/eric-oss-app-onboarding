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

import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DESCRIPTOR_APPCOMPONENT_OR_APPCOMPONENT_LIST;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.consts.ParserConstants;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.CreateAppRequest;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils;

@SpringBootTest(classes = { CoreApplication.class, CsarParserService.class })
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class CsarParserServiceTest {

    @Autowired
    private CsarParserService csarParserService;
    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;
    @Autowired
    private DecompressorService decompressorService;
    @Autowired
    private OnboardingJobRepository onboardingJobRepository;
    private static OnboardingJobEntity onboardingJobSingleComponent;
    private static OnboardingJobEntity onboardingJobWithProviderSingleComponent;

    private static boolean isInitialized = false;
    private static String tmpDirPath;
    private static final String TEST_FILE_CONTENT = "Test-Key: This is a test value";
    private static final String APP_DESCRIPTOR_FILE_PATH = "Definitions/AppDescriptor.yaml";

    @BeforeEach
    public void init() throws URISyntaxException, IOException {
        if (!isInitialized) {
            onboardingJobSingleComponent = decompressCsar(TEST_CSAR_NAME, TEST_CSAR_LOCATION);
            onboardingJobWithProviderSingleComponent = decompressCsar(TEST_CSAR_WITH_PROVIDER_NAME, TEST_CSAR_WITH_PROVIDER_LOCATION);
            tmpDirPath = tempFolderLocation;
            isInitialized = true;
        }
    }

    @AfterEach
    public void restoreFiles() throws IOException {
        FileUtils.revertRenamedFile(tempFolderLocation, onboardingJobSingleComponent.getId(),
            Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR));
        FileUtils.revertRenamedFile(tempFolderLocation, onboardingJobSingleComponent.getId(), Path.of(APP_DESCRIPTOR_FILE_PATH));
    }

    @AfterAll
    public static void wipeOutTempFolder() throws IOException {
        FileUtils.cleanUpTempAllDirectories(tmpDirPath);
    }

    @Test
    public void testParseCsar_Success_withMainAppRequestProperties() {
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertNotNull(appRequest.getName());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_NAME, appRequest.getName());
            Assertions.assertNotNull(appRequest.getVersion());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_VERSION, appRequest.getVersion());
            Assertions.assertNotNull(appRequest.getType());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_TYPE, appRequest.getType());
            Assertions.assertNotNull(appRequest.getProvider());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_PROVIDER, appRequest.getProvider());

            Assertions.assertNotNull(appRequest.getComponents());
            Assertions.assertTrue(appRequest.getComponents().size() > 0);
            Assertions.assertNotNull(appRequest.getPermissions());
            Assertions.assertTrue(appRequest.getPermissions().size() > 0);
            Assertions.assertNotNull(appRequest.getRoles());
            Assertions.assertTrue(appRequest.getRoles().size() > 0);
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_Success_with_provider_MainAppRequestProperties() {
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobWithProviderSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertNotNull(appRequest.getName());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_NAME, appRequest.getName());
            Assertions.assertNotNull(appRequest.getVersion());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_VERSION, appRequest.getVersion());
            Assertions.assertNotNull(appRequest.getType());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_TYPE, appRequest.getType());
            Assertions.assertNotNull(appRequest.getProvider());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_PROVIDER, appRequest.getProvider());

            Assertions.assertNotNull(appRequest.getComponents());
            Assertions.assertTrue(appRequest.getComponents().size() > 0);
            Assertions.assertNotNull(appRequest.getPermissions());
            Assertions.assertTrue(appRequest.getPermissions().size() > 0);
            Assertions.assertNotNull(appRequest.getRoles());
            Assertions.assertTrue(appRequest.getRoles().size() > 0);
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_Success_withAppRequestPermissions() {
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertNotNull(appRequest.getPermissions());
            Assertions.assertTrue(appRequest.getPermissions().size() > 0);
            Assertions.assertNotNull(appRequest.getPermissions().get(0).getResource());
            Assertions.assertNotNull(appRequest.getPermissions().get(0).getScope());

            Assertions.assertNotNull(appRequest.getComponents());
            Assertions.assertTrue(appRequest.getComponents().size() > 0);
            Assertions.assertNotNull(appRequest.getRoles());
            Assertions.assertTrue(appRequest.getRoles().size() > 0);
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_Success_withNoAppRequestPermissions() throws IOException {
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), Path.of(APP_DESCRIPTOR_FILE_PATH),
                ParserConstants.DESCRIPTOR_APP_PERMISSIONS);

        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertTrue(appRequest.getPermissions().isEmpty());
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_Success_withEmptyScopeForKafkaResourcePermissions() throws IOException {
        FileUtils.replaceTextWithGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), Path.of(APP_DESCRIPTOR_FILE_PATH),
                "Scope: test", "Scope: ");

        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertFalse(appRequest.getPermissions().isEmpty());
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_failed_withEmptyScopeForBDRResourcePermissions() throws IOException {
        FileUtils.replaceTextWithGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), Path.of(APP_DESCRIPTOR_FILE_PATH),
                "Scope: read", "Scope: ");

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        Assertions.assertTrue(
            exception.getProblemDetails().getDetail().contains(String.format(ErrorMessages.PARSE_APP_PERMISSIONS_EMPTY_SCOPE_ERROR, "bdr")));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getResponseStatus());
    }

    @Test
    public void testParseCsar_Success_withAppRequestRoles() {
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertNotNull(appRequest.getRoles());
            Assertions.assertTrue(appRequest.getRoles().size() > 0);
            Assertions.assertNotNull(appRequest.getRoles().get(0).getName());
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_Success_withNoAppRequestRoles() throws IOException {
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), Path.of(APP_DESCRIPTOR_FILE_PATH),
                ParserConstants.DESCRIPTOR_APP_ROLES);
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertTrue(appRequest.getRoles().isEmpty());
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_Success_withAppRequestComponents() {
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertNotNull(appRequest.getComponents());
            Assertions.assertTrue(appRequest.getComponents().size() > 0);
            Assertions.assertNotNull(appRequest.getComponents().get(0).getType());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_COMPONENT_TYPE, appRequest.getComponents().get(0).getType());
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCar_success_withASDArtifactType() {
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertTrue(appRequest.getComponents().size() > 0);
            Assertions.assertEquals(Constants.TEST_CSAR_APP_COMPONENT_TYPE, appRequest.getComponents().get(0).getType());
            final String appComponents = getAppComponentFromTestCsar();
            Assertions.assertTrue(appComponents.contains(appRequest.getComponents().get(0).getName()));
            Assertions.assertTrue(appComponents.contains(appRequest.getComponents().get(0).getVersion()));
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCar_success_withASDHelmAndImageArtifacts() {
        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertTrue(appRequest.getComponents().size() > 0);
            Assertions.assertEquals(Constants.TEST_CSAR_APP_COMPONENT_TYPE, appRequest.getComponents().get(0).getType());
            Assertions.assertTrue(appRequest.getComponents().get(0).getArtifacts().size() >= 2);
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_failed_toscaFileNotExist() throws IOException {
        final Path toscaFilePath = Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        FileUtils.renameFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), toscaFilePath);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_FILE_NOT_EXIST_ERROR, "", TEST_CSAR_NAME);
    }

    @Test
    public void testParseCsar_failed_toscaFileNotReadable() throws IOException {
        final Path toscaFilePath = Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        FileUtils.replaceFileWithContentInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), toscaFilePath, Constants.MALFORMED_BYTES);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_CSAR_ERROR, TEST_CSAR_NAME, "");
    }

    @Test
    public void testParseCsar_failed_toscaFileInvalidFormat() throws IOException {
        final Path toscaFilePath = Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        FileUtils.replaceFileWithContentInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), toscaFilePath, REPLACEMENT_TEXT.getBytes());

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_YAML_ERROR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
    }

    @Test
    public void testParseCsar_failed_toscaFileInvalidYamlFormat() throws IOException {
        final Path toscaFilePath = Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        FileUtils.replaceTextWithGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), toscaFilePath, ParserConstants.TOSCA_CREATED_BY,
            "  " + ParserConstants.TOSCA_CREATED_BY);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_YAML_ERROR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
    }

    @Test
    public void testParseCsar_failed_toscaFileWithNoVendor() throws IOException {
        final Path toscaFilePath = Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        FileUtils.replaceFileWithContentInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), toscaFilePath, TEST_FILE_CONTENT.getBytes());

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, "", ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
    }

    @Test
    public void testParseCsar_failed_toscaFileWithNoDescriptorPath() throws IOException {
        final Path toscaFilePath = Path.of(ParserConstants.METADATA_FOLDER_NAME_IN_CSAR, ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), toscaFilePath, ParserConstants.TOSCA_ENTRY_DEFINITIONS);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.TOSCA_ENTRY_DEFINITIONS,
                ParserConstants.TOSCA_FILE_NAME_IN_CSAR);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileNotExist() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.renameFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_FILE_NOT_EXIST_ERROR, APP_DESCRIPTOR_FILE_PATH, TEST_CSAR_NAME);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileNotReadable() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceFileWithContentInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath, Constants.MALFORMED_BYTES);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_CSAR_ERROR, TEST_CSAR_NAME, "");
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileInvalidFormat() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceFileWithContentInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath, REPLACEMENT_TEXT.getBytes());

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_APP_DESCRIPTOR_YAML_IN_CSAR_ERROR, TEST_CSAR_NAME, "");
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileHavingInvalidYamlFormat() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceTextWithGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
            ParserConstants.DESCRIPTOR_APP_NAME, "  " + ParserConstants.DESCRIPTOR_APP_NAME);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_YAML_ERROR, descriptorFilePath.getFileName().toString());
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoAppName() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath, ParserConstants.DESCRIPTOR_APP_NAME);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.DESCRIPTOR_APP_NAME,
                APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithDuplicateAppName() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        final String duplicatedAPPName = ParserConstants.DESCRIPTOR_APP_NAME + ": This-is-a-test\n  " + ParserConstants.DESCRIPTOR_APP_NAME;
        FileUtils.replaceTextWithGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
            ParserConstants.DESCRIPTOR_APP_NAME, duplicatedAPPName);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, descriptorFilePath.getFileName().toString(), ParserConstants.DESCRIPTOR_APP_NAME);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoAppVersion() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
                ParserConstants.DESCRIPTOR_APP_VERSION);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.DESCRIPTOR_APP_VERSION,
                APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoAppType() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath, ParserConstants.DESCRIPTOR_APP_TYPE);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.DESCRIPTOR_APP_TYPE,
                APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoAppComponent() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
                ParserConstants.DESCRIPTOR_APPCOMPONENT);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, DESCRIPTOR_APPCOMPONENT_OR_APPCOMPONENT_LIST,
                APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoArtifactType() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
                ParserConstants.DESCRIPTOR_APPCOMPONENT_ARTIFACTTYPE);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR,
                ParserConstants.DESCRIPTOR_APPCOMPONENT_ARTIFACTTYPE, APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCar_failed_appDescriptorFileWithUnknownArtifactType() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath, "Microservice");

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.NONE_SUPPORTED_ARTIFACT_PARSER_ERROR, REPLACEMENT_TEXT);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoComponentName() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
                ParserConstants.APP_COMPONENT_NAME_OF_COMPONENT);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.APP_COMPONENT_NAME_OF_COMPONENT,
                APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoComponentVersion() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
                ParserConstants.APP_COMPONENT_VERSION);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, ParserConstants.APP_COMPONENT_VERSION,
                APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithNoComponentPath() throws IOException {
        final Path descriptorFilePath = Path.of(APP_DESCRIPTOR_FILE_PATH);
        FileUtils.replaceGivenTextInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), descriptorFilePath,
                ParserConstants.DESCRIPTOR_APPCOMPONENT_ARTIFACT_FILE_PATH);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.ARTIFACT_FILE_PATH_NOT_EXIST_ERROR, APP_DESCRIPTOR_FILE_PATH);
    }

    @Test
    public void testParseCsar_success_appDescriptorFileWithArrayOfComponents() throws IOException {
        replaceAppComponentWithArray();

        final Executable executable = () -> {
            final CreateAppRequest appRequest = csarParserService.parseCsar(onboardingJobSingleComponent);

            Assertions.assertNotNull(appRequest);
            Assertions.assertNotNull(appRequest.getComponents());
            Assertions.assertEquals(2, appRequest.getComponents().size());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_COMPONENT_TYPE, appRequest.getComponents().get(0).getType());
            Assertions.assertEquals(Constants.TEST_CSAR_APP_COMPONENT_TYPE, appRequest.getComponents().get(1).getType());
        };

        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithUnknownComponentDef() throws IOException {
        replaceAppComponentWithString();

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.UNKNOWN_APPCOMPONENT_DEFINITION_ERROR);
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithUnknownPermissionsDef() throws IOException {
        FileUtils.replaceYamlArrayInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), APP_DESCRIPTOR_FILE_PATH,
                ParserConstants.DESCRIPTOR_APP_PERMISSIONS);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_APP_DESCRIPTOR_YAML_IN_CSAR_ERROR, onboardingJobSingleComponent.getFileName(), "");
    }

    @Test
    public void testParseCsar_failed_appDescriptorFileWithUnknownRolesDef() throws IOException {
        FileUtils.replaceYamlArrayInFileInJobDir(tempFolderLocation, onboardingJobSingleComponent.getId(), APP_DESCRIPTOR_FILE_PATH,
                ParserConstants.DESCRIPTOR_APP_ROLES);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(onboardingJobSingleComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_APP_DESCRIPTOR_YAML_IN_CSAR_ERROR, onboardingJobSingleComponent.getFileName(), "");
    }

    @Test
    public void testParseCsar_failed_AppComponentList_withInvalidComponentType() throws IOException, URISyntaxException {
        OnboardingJobEntity invalidComponent = decompressCsar(TEST_MULTPLE_COMPONENT_CSAR_NAME, TEST_MULTPLE_COMPONENT_CSAR_LOCATION);

        replaceYamlText(invalidComponent, DATA_MANAGEMENT_ARTEFACT_TYPE, DATA_MANAGEMENT_INAVALID_ARTEFACT_TYPE);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(invalidComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.NONE_SUPPORTED_ARTIFACT_PARSER_ERROR, "Invalid");
    }

    @Test
    public void testParseCsar_failed_AppComponentList_withInvalidComponentPath() throws IOException, URISyntaxException {
        OnboardingJobEntity invalidComponent = decompressCsar(TEST_CSAR_MULTIPLE_COMPONENT_INVALID_PATH_NAME, TEST_CSAR_MULTIPLE_COMPONENT_INVALID_PATH_LOCATION);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(invalidComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_FILE_NOT_EXIST_ERROR, "OtherDefinitions/DataInvalidPath", TEST_CSAR_MULTIPLE_COMPONENT_INVALID_PATH_NAME);
    }

    @Test
    public void testParseCsar_failed_AppComponentList_withInvalidComponentList() throws IOException, URISyntaxException {
        OnboardingJobEntity invalidComponent = decompressCsar(TEST_CSAR_MULTIPLE_COMPONENT_INVALID_LIST_NAME, TEST_CSAR_MULTIPLE_COMPONENT_INVALID_LIST_LOCATION);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(invalidComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSE_YAML_ERROR, "AppDescriptor.yaml");
    }

    @Test
    public void testParseCsar_failed_AppComponentList_withEmptyComponentList() throws IOException, URISyntaxException {
        OnboardingJobEntity invalidComponent = decompressCsar(TEST_CSAR_NO_COMPONENT_NAME, TEST_CSAR_NO_COMPONENT_LOCATION);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(invalidComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, DESCRIPTOR_APPCOMPONENT_OR_APPCOMPONENT_LIST, APP_DESCRIPTOR_LOCATION_IN_CSAR_WITHOUT_INITIAL_SLASH);
    }

    @Test
    public void testParseCsar_failed_AppComponentList_withDoubleComponentType() throws IOException, URISyntaxException {
        OnboardingJobEntity invalidComponent = decompressCsar(TEST_APP_DESCRIPTOR_BOTH_COMPONENTS_NAME, TEST_APP_DESCRIPTOR_BOTH_COMPONENTS_LOCATION);

        final OnboardingJobException exception = Assertions.assertThrows(OnboardingJobException.class, () -> {
            csarParserService.parseCsar(invalidComponent);
        });

        TestUtils.assertInternalServerError(exception, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, DESCRIPTOR_APPCOMPONENT_OR_APPCOMPONENT_LIST, APP_DESCRIPTOR_LOCATION_IN_CSAR_WITHOUT_INITIAL_SLASH);
    }

    private String getAppComponentFromTestCsar() throws IOException {
        final Path baseDirPath = Paths.get(tempFolderLocation, onboardingJobSingleComponent.getId().toString());
        final Path appDescriptorFilePath = baseDirPath.resolve(APP_DESCRIPTOR_FILE_PATH);
        String fileData = Files.readString(appDescriptorFilePath);
        fileData = fileData.substring(fileData.indexOf(ParserConstants.DESCRIPTOR_APPCOMPONENT),
                fileData.indexOf(ParserConstants.DESCRIPTOR_APP_PERMISSIONS) - 1);

        return fileData;
    }

    private void replaceAppComponentWithString() throws IOException {
        final Path baseDirPath = Paths.get(tempFolderLocation, onboardingJobSingleComponent.getId().toString());
        final Path appDescriptorFilePath = baseDirPath.resolve(APP_DESCRIPTOR_FILE_PATH);

        String fileData = Files.readString(appDescriptorFilePath);
        final String replacementText = ParserConstants.DESCRIPTOR_APPCOMPONENT + ": " + REPLACEMENT_TEXT;
        fileData = fileData.replace(getAppComponentFromTestCsar(), replacementText);

        final Path targetFilePath = baseDirPath.resolve(appDescriptorFilePath + REPLACEMENT_FILENAME_SUFFIX);
        Files.move(appDescriptorFilePath, targetFilePath);
        Files.write(appDescriptorFilePath, fileData.getBytes());
    }

    private void replaceYamlText(OnboardingJobEntity entity, String originalText, String replacementText) throws IOException {
        final Path baseDirPath = Paths.get(tempFolderLocation, entity.getId().toString());
        final Path appDescriptorFilePath = baseDirPath.resolve(APP_DESCRIPTOR_FILE_PATH);
        String fileData = Files.readString(appDescriptorFilePath);
        fileData = fileData.replace(originalText, replacementText);

        Files.writeString(appDescriptorFilePath, fileData);
    }

    private void replaceAppComponentWithArray() throws IOException {
        final Path baseDirPath = Paths.get(tempFolderLocation, onboardingJobSingleComponent.getId().toString());
        final Path appDescriptorFilePath = baseDirPath.resolve(APP_DESCRIPTOR_FILE_PATH);

        final String fileData = Files.readString(appDescriptorFilePath);
        final String appComponent = fileData.substring(fileData.indexOf(ParserConstants.DESCRIPTOR_APPCOMPONENT),
                fileData.indexOf(ParserConstants.DESCRIPTOR_APP_PERMISSIONS) - 1);

        String appComponentArr = appComponent.replace(ParserConstants.DESCRIPTOR_APPCOMPONENT + ":\n", "");
        appComponentArr = appComponentArr.replaceAll(ParserConstants.APP_COMPONENT_NAME_OF_COMPONENT,
                "- " + ParserConstants.APP_COMPONENT_NAME_OF_COMPONENT);
        appComponentArr = appComponentArr.replaceAll("\n  ", "\n    ");
        appComponentArr = ParserConstants.DESCRIPTOR_APPCOMPONENT + ":\n" + appComponentArr + "\n" + appComponentArr;

        final Path targetFilePath = baseDirPath.resolve(appDescriptorFilePath + REPLACEMENT_FILENAME_SUFFIX);
        Files.move(appDescriptorFilePath, targetFilePath);
        Files.write(appDescriptorFilePath, fileData.replace(appComponent, appComponentArr).getBytes());
    }

    private OnboardingJobEntity decompressCsar(String csarName, String csarLocation) throws URISyntaxException, IOException {
        final OnboardingJobEntity entity = onboardingJobRepository
            .save(OnboardingJobEntity.builder().fileName(csarName).status(OnboardingJobStatus.UNPACKED).build());

        final Path tmpPath = Paths.get(tempFolderLocation);
        FileUtils.copyFileToFolder(tmpPath, csarName, csarLocation);
        decompressorService.decompressCsarPackage(csarName, entity.getId());
        return entity;
    }
}
