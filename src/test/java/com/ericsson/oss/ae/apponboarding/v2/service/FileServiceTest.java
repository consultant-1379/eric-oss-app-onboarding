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

import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DELETE_FROM_FILESYSTEM_ERROR;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_LOCATION;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = { CoreApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class FileServiceTest {

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;

    @Autowired
    private FileService fileService;

    @Test
    public void removeFileFromLocalFilesystem_throwsIOException() {
        try (final MockedStatic<Files> theMock = Mockito.mockStatic(Files.class)) {
            theMock.when(() -> Files.deleteIfExists(Paths.get(tempFolderLocation, TEST_CSAR_LOCATION))).thenThrow(new IOException("IO Error"));

            final OnboardingJobException exception = assertThrows(OnboardingJobException.class, () -> {
                fileService.removeFileFromLocalFilesystem(TEST_CSAR_LOCATION);
            });

            final String expectedMessage = String.format(DELETE_FROM_FILESYSTEM_ERROR, TEST_CSAR_LOCATION, "IO Error");
            assertTrue(exception.getProblemDetails().getDetail().equals(expectedMessage));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, (exception).getResponseStatus());
        }
    }
}
