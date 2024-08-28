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
import java.nio.file.*;
import java.util.ServiceConfigurationError;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.function.ThrowingConsumer;

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

/**
 * Handles the decompression of the csar archive and extraction of the content.
 */
@Service
public class DecompressorService {

    private static final Logger logger = LoggerFactory.getLogger(DecompressorService.class);

    @Value("${onboarding-job.tempFolderLocation}") private String tempFolderLocation;

    public void decompressCsarPackage(@Nonnull final String csarName, @Nonnull final UUID jobId) {
        logger.info("decompressCsarPackage() Decompressing csar package {} for onboarding-job {}", csarName, jobId);

        this.decompressFile(csarName, jobId.toString());
    }

    private void decompressFile(final String csarName, final String targetDirName) {
        final Path tmpPath = Paths.get(this.tempFolderLocation);
        final Path pathToZipFile = tmpPath.resolve(csarName);
        final Path destinationFolder = tmpPath.resolve(targetDirName);

        if (Files.exists(pathToZipFile)) {
            try (FileSystem zipFileSystem = FileSystems.newFileSystem(pathToZipFile, DecompressorService.class.getClassLoader())) {
                Path root = zipFileSystem.getPath(Consts.FORWARD_SLASH);
                this.extractFilesToDisk(csarName, root, destinationFolder);
            } catch (ProviderNotFoundException | ServiceConfigurationError | IOException | SecurityException e) {
                logger.error("decompressFile() unable to read zip file. Reason: {}", e.getMessage());
                throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.DECOMPRESS_CSAR_PROBLEM_TITLE,
                        String.format(ErrorMessages.DECOMPRESS_CSAR_ERROR, csarName, e.getMessage()));
            }
        } else {
            logger.error("decompressFile() Zip file does not exist.");
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.DECOMPRESS_CSAR_PROBLEM_TITLE,
                    String.format(ErrorMessages.DECOMPRESS_CSAR_NOT_AVAILABLE_IN_TMP_ERROR, csarName, this.tempFolderLocation));
        }
    }

    private void extractFilesToDisk(final String csarName, final Path root, final Path destinationFolder) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(path -> !Files.isDirectory(path)).forEach(ThrowingConsumer.of(path -> copyFileToDisk(path, destinationFolder)));
        } catch (Exception e) {
            logger.error("extractFilesToDisk() unable to extract files in zip file to disk. Reason: {}", e.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.DECOMPRESS_CSAR_PROBLEM_TITLE,
                    String.format(ErrorMessages.DECOMPRESS_CSAR_EXTRACT_ERROR, csarName, e.getMessage()));
        }
    }

    private void copyFileToDisk(final Path fileToExtract, final Path destinationFolder) throws IOException {
        Path destinationPath = destinationFolder.resolve(fileToExtract.toString().substring(1));
        Files.createDirectories(destinationPath.getParent());
        Files.copy(fileToExtract, destinationPath);
        logger.debug("copyFileToDisk() File copied successfully: {}", destinationPath);
    }
}
