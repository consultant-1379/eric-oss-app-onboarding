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

package com.ericsson.oss.ae.apponboarding.v2.robustness;

import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.ObjectStoreService;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.MinioBucketInitializer;
import io.minio.MinioClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MinioBucketInitializerTest {

    @Mock
    private MinioClient minioClient;
    @Mock
    private ObjectStoreService objectStoreService;

    @InjectMocks
    private MinioBucketInitializer minioBucketInitializer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(minioBucketInitializer, "bucketName", "app-management");
    }

    @Test
    public void testInitializeBucketCreation() {
        // Mock behavior to simulate that bucket doesn't exist initially
        when(objectStoreService.doesBucketExist()).thenReturn(false);

        // Call the method to be tested
        minioBucketInitializer.initializeBucket();

        // Verify that createBucket() method is called
        verify(objectStoreService, times(1)).createBucket();
    }

    @Test
    public void testInitializeBucketAlreadyExists() {
        // Mock behavior to simulate that bucket exist initially
        when(objectStoreService.doesBucketExist()).thenReturn(true);

        // Call the method to be tested
        minioBucketInitializer.initializeBucket();

        // Verify that createBucket() method is not called
        verify(objectStoreService, never()).createBucket();
    }

    @Test
    public void testInitializeBucketThrowsException() {
        // Mock the behaviour to throw exception when checking the bucket exists
        when(objectStoreService.doesBucketExist()).thenThrow(new OnboardingJobException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable"));

        Assertions.assertThrows(OnboardingJobException.class, () -> {
            minioBucketInitializer.initializeBucket();
        });
        verify(objectStoreService, never()).createBucket();
    }

}