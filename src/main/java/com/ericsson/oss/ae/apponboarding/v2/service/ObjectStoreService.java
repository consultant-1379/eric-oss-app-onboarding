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

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FORWARD_SLASH;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.BUCKET_EXISTS_FAILURE_DETAIL;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.BUCKET_EXISTS_FAILURE_TITLE;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DELETE_BUCKET_STORE_ARTIFACT_FAILURE_TITLE;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.FAILED_TO_CREATE_BUCKET_DETAIL;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.FAILED_TO_DELETE_ARTIFACTS_DETAIL;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.FAILED_TO_STORE_ARTIFACTS_TITLE;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.FAILED_TO_STORE_ARTIFACT_FAILURE_DETAIL;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Artifact;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import io.minio.messages.Item;

/**
 * Service that integrates with the object-store-mn microservice
 *
 */
@Service
public class ObjectStoreService {

    private static final Logger logger = LoggerFactory.getLogger(ObjectStoreService.class);

    @Autowired
    private MinioClient minioClient;


    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;

    @Value("${object-store.bucketName}")
    private String bucketName;

    /**
     * Creates a new bucket in the object store using the provided bucket name.
     *
     * @throws OnboardingJobException If there's an error during bucket creation
     */
    public void createBucket() {
        try {
            logger.info("createBucket() Creating bucket: {}", bucketName);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (final MinioException | IOException | GeneralSecurityException e) {
            logger.error("createBucket() Error while creating bucket: {}", bucketName, e);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, FAILED_TO_STORE_ARTIFACTS_TITLE, FAILED_TO_CREATE_BUCKET_DETAIL);
        }
    }

    public boolean doesBucketExist() {
        try{
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (final MinioException | IOException | GeneralSecurityException e) {
            logger.error("doesBucketExist() Error checking if bucket exists: {}. Reason: {}", bucketName, e.getMessage(), e);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, BUCKET_EXISTS_FAILURE_TITLE, BUCKET_EXISTS_FAILURE_DETAIL);

        }
    }

    /**
     * Uploads the specified artifact to the object store under the provided bucket.
     * The object name (location) for the uploaded object will be composed of the jobId and artifact name, 'jobId/artifactName'
     *
     * @param jobId The unique identifier (UUID) of the onboarding-job.
     * @param artifact   The artifact to be uploaded, containing details like name, type, and location.
     * @throws OnboardingJobException If there's an error during the upload process
     */
    public void uploadArtifact(final UUID jobId, final Artifact artifact) {
        logger.debug("uploadArtifact() Uploading artifact: {} from location: {} to object store", artifact.getName(), artifact.getLocation());
        try {
            final Path location = Path.of(tempFolderLocation, jobId.toString(), artifact.getLocation());
            final String objectName = jobId.toString() + FORWARD_SLASH + location.getFileName().toString();

            minioClient.uploadObject(UploadObjectArgs.builder().bucket(bucketName)
                .object(objectName).filename(location.toString()).build());
            artifact.setLocation(bucketName + FORWARD_SLASH + objectName);

        } catch (final MinioException | IOException | GeneralSecurityException| IllegalArgumentException e) {
            logger.error("uploadArtifact() Error while pushing artifact to storage: {}", artifact.getName(), e);
            final String detail = String.format(FAILED_TO_STORE_ARTIFACT_FAILURE_DETAIL, artifact.getType(), artifact.getName());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, FAILED_TO_STORE_ARTIFACTS_TITLE, detail);
        }
   }
    public void deleteObjects(final UUID jobId) {
        if(doesBucketExist()) {
            final Iterable<Result<Item>> artifacts = getArtifacts(jobId);
            removeArtifacts(artifacts, jobId);
        }
    }

    private Iterable<Result<Item>> getArtifacts(final UUID jobId)
    {
        logger.debug("getArtifacts() Retrieving list of artifacts from bucket {} with prefix {}", bucketName, jobId);

        // List objects with the prefix
        final ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(jobId.toString())
                .recursive(true)
                .build();

        return minioClient.listObjects(listObjectsArgs);
    }

    private void removeArtifacts(final Iterable<Result<Item>> artifacts, final UUID jobId) {
        logger.debug("removeArtifacts() Removing all artifacts from bucket {} with prefix {}", bucketName, jobId);
        String itemName = null;
        try {
            for (final Result<Item> result : artifacts) {
                itemName = result.get().objectName();
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(itemName).build());
                logger.debug("removeArtifacts() Removed artifact {} from bucket", itemName);
            }
        } catch (final MinioException | IOException | GeneralSecurityException | IllegalArgumentException e) {
            logger.error("removeArtifacts() Error while deleting artifact with name {} from bucket: {}", itemName, bucketName, e);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, DELETE_BUCKET_STORE_ARTIFACT_FAILURE_TITLE, FAILED_TO_DELETE_ARTIFACTS_DETAIL);
        }
    }

}
