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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Value("${onboarding-job.tempFolderLocation}") private String tempFolderLocation;

    public void saveFileToLocalFilesystem(final MultipartFile multipartFile) {
        final Path destinationFile = Paths.get(tempFolderLocation, multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(destinationFile);
        } catch (final IOException ex) {
            final String errorDetail = String.format(ErrorMessages.SAVE_TO_FILESYSTEM_ERROR, multipartFile.getName(), ex.getMessage());
            logger.error(errorDetail, ex);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, errorDetail);
        }
    }

    public void removeFileFromLocalFilesystem(final String filename) {
        if (StringUtils.isBlank(filename)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(tempFolderLocation, filename));
        } catch (final IOException ex) {
            final String errorDetail = String.format(ErrorMessages.DELETE_FROM_FILESYSTEM_ERROR, filename, ex.getMessage());
            logger.error(errorDetail, ex);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, errorDetail);
        }
    }

    public void removeFolderFromLocalFilesystem(final String folderToRemove) {
        if (StringUtils.isBlank(folderToRemove)) {
            return;
        }

        try {
            final Path folderToDelete = Paths.get(tempFolderLocation, folderToRemove);
            if (Files.exists(folderToDelete)) {
                try (Stream<Path> walk = Files.walk(folderToDelete)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }

                Files.deleteIfExists(folderToDelete);
            }
        } catch (final IOException e) {
            final String errorDetail = String.format(ErrorMessages.DELETE_FROM_FILESYSTEM_ERROR, folderToRemove, e.getMessage());
            logger.error(errorDetail, e);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, errorDetail);
        }
    }
}
