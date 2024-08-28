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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.utils.Constants;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, DecompressorService.class })
@ActiveProfiles("test")
public class DecompressorServiceTest {
    @Autowired
    DecompressorService decompressorService;

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;

    private UUID jobId;

    @AfterEach
    public void cleanUp() throws IOException {
        FileUtils.cleanUpFilesCreated(tempFolderLocation, jobId);
    }

    @Test
    void testDecompressCsarPackage_success() throws URISyntaxException, IOException {
        // Given
        final Path tmpPath = Paths.get(tempFolderLocation);
        jobId = UUID.randomUUID();
        FileUtils.copyFileToFolder(tmpPath, Constants.TEST_CSAR_NAME, Constants.TEST_CSAR_LOCATION);

        // When
        Assertions.assertDoesNotThrow(() -> {
            decompressorService.decompressCsarPackage(Constants.TEST_CSAR_NAME, jobId);
        });

        // Then
        Assertions.assertTrue(Files.exists(tmpPath.resolve(jobId.toString())));
        Assertions.assertTrue(Files.exists(tmpPath.resolve(jobId + Constants.APP_DESCRIPTOR_LOCATION_IN_CSAR)));
    }

    @Test
    void testDecompressCsarPackage_fail_corruptCsar() throws URISyntaxException, IOException {
        final Path tmpPath = Paths.get(tempFolderLocation);
        // Given
        FileUtils.copyFileToFolder(tmpPath, Constants.INVALID_ARCHIVE_CSAR_NAME, Constants.INVALID_ARCHIVE_CSAR_LOCATION);
        jobId = UUID.randomUUID();

        // When
        Assertions.assertThrows(OnboardingJobException.class, () -> {
            decompressorService.decompressCsarPackage(Constants.INVALID_ARCHIVE_CSAR_NAME, jobId);
        });

        // Then
        Assertions.assertTrue(Files.notExists(tmpPath.resolve(jobId.toString())));
    }

    @Test
    void testDecompressCsarPackage_fail_fileCopyError() throws URISyntaxException, IOException {
        // Given
        jobId = UUID.randomUUID();
        final Path tmpPath = Paths.get(tempFolderLocation);
        FileUtils.copyFileToFolder(tmpPath, Constants.TEST_CSAR_NAME, Constants.TEST_CSAR_LOCATION);
        final Path pathToDescriptor = Path.of(tempFolderLocation, jobId.toString(), Constants.APP_DESCRIPTOR_LOCATION_IN_CSAR);
        Files.createDirectories(pathToDescriptor.getParent());
        Files.write(pathToDescriptor, "Hello".getBytes());

        // When
        Assertions.assertThrows(OnboardingJobException.class, () -> {
            decompressorService.decompressCsarPackage(Constants.TEST_CSAR_NAME, jobId);
        });

        // Then
        Assertions.assertTrue(Files.exists(tmpPath.resolve(jobId.toString())));
    }
}
