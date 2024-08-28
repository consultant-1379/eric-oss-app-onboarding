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
import java.net.ConnectException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Artifact;
import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.CreateAppRequest;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;
import com.ericsson.oss.ae.apponboarding.v2.utils.TestUtils;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InternalException;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class ArtifactStorageServiceTest {

    @Autowired
    private ObjectStoreService objectStoreService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OnboardingJobRepository onboardingJobRepository;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @MockBean
    private MinioClient minioClient;

    @Value("${onboarding-job.tempFolderLocation}")
    private String tempFolderLocation;

    @Value("${object-store.bucketName}")
    private String bucketName;

    protected MockitoSession session;
    private MockMvc mvc;

    private UUID jobId;

    @AfterEach
    public void cleanUp() throws IOException {
        if (jobId != null) {
            FileUtils.cleanUpFilesCreated(tempFolderLocation, jobId);
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        TestUtils.createDirForCsarUpload(tempFolderLocation);
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    public void tearDown() {
        if (session != null) {
            session.finishMocking();
        }
    }

    @Test
    public void testBuildBucket_MinioException_Error() throws Exception {
        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(mockJob.getId().toString(), true);
        Mockito.doThrow(new InternalException("Error creating bucket", "HTTP error"))
            .when(minioClient)
            .makeBucket(Mockito.any(MakeBucketArgs.class));

        // When & Then
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.storeArtifacts(mockJob, mockRequest));
    }

    @Test
    public void testBuildBucket_IOException_Error() throws Exception {
        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(mockJob.getId().toString(), true);
        Mockito.doThrow(new IOException("Error transferring bucketId"))
            .when(minioClient)
            .makeBucket(Mockito.any(MakeBucketArgs.class));

        // When & Then
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.storeArtifacts(mockJob, mockRequest));

    }

    @Test
    public void testBuildBucket_SecurityException_Error() throws Exception {
        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(mockJob.getId().toString(), true);
        Mockito.doThrow(new InvalidKeyException("Error authenticating when creating bucket"))
            .when(minioClient)
            .makeBucket(Mockito.any(MakeBucketArgs.class));

        // When & Then
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.storeArtifacts(mockJob, mockRequest));
    }

    @Test
    public void testUploadObject_MinIOException_Error() throws Exception {
        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(mockJob.getId().toString(), true);
        Mockito.doThrow(new InternalException("Error creating artifact","Http trace"))
            .when(minioClient)
            .uploadObject(Mockito.any(UploadObjectArgs.class));

        // When & Then
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.storeArtifacts(mockJob, mockRequest));
    }

    @Test
    public void testBuildBucket_invalidBucketName() throws Exception {

        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(UUID.randomUUID().toString(), true);

        Mockito.doThrow(new IllegalArgumentException()).when(minioClient).makeBucket(Mockito.mock(MakeBucketArgs.class));

        Assertions.assertThrows(OnboardingJobException.class, () -> artifactStorageService.storeArtifacts(mockJob, mockRequest));
    }

    @Test
    public void testUploadObject_IOException_Error() throws Exception {
        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(mockJob.getId().toString(), true);
        Mockito.doNothing().when(minioClient).makeBucket(Mockito.any(MakeBucketArgs.class));
        Mockito.doThrow(new IOException("Error transferring bucketId"))
            .when(minioClient)
            .uploadObject(Mockito.any(UploadObjectArgs.class));

        // When & Then
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.storeArtifacts(mockJob, mockRequest));
    }

    @Test
    public void testUploadObject_SecurityException_Error() throws Exception {
        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(mockJob.getId().toString(), true);
        Mockito.doThrow(new InvalidKeyException("Error authenticating when creating artifact"))
            .when(minioClient)
            .uploadObject(Mockito.any(UploadObjectArgs.class));

        // When & Then
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.storeArtifacts(mockJob, mockRequest));
    }

    @Test
    public void testUploadObject_FileNotFound_Error() throws Exception {
        final OnboardingJobEntity mockJob = mockOnboardingJobEntity();
        final CreateAppRequest mockRequest = mockCreateAppRequest(mockJob.getId().toString(), false);
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(mockJob.getId().toString()).build();

        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(false);

        // When & Then
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.storeArtifacts(mockJob, mockRequest));
    }

    @Test
    public void testDelete_BucketNotFound() throws Exception{
        final UUID jobId = UUID.randomUUID();
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket("random-bucket").build();

        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(false);

        Assertions.assertDoesNotThrow(()
            -> artifactStorageService.deleteArtifacts(jobId));
    }

    @Test
    public void testDelete_GetArtifacts_UnauthorizedAccess_Error() throws Exception {
        final UUID jobId = UUID.randomUUID();
        final ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().recursive(true).bucket(bucketName).prefix(jobId.toString()).build();
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();

        final Iterable<Result<Item>> mockIterable = Mockito.mock(Iterable.class);
        final Iterator<Result<Item>> mockIterator = Mockito.mock(Iterator.class);
        final Result<Item> mockResult = Mockito.mock(Result.class);

        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(true);
        Mockito.when(minioClient.listObjects(listObjectsArgs)).thenReturn(mockIterable);
        Mockito.when(mockIterable.iterator()).thenReturn(mockIterator);
        Mockito.when(mockIterator.hasNext()).thenReturn(true);
        Mockito.when(mockIterator.next()).thenReturn(mockResult);

        final ErrorResponse mockErrorResponse = Mockito.mock(ErrorResponse.class);
        Mockito.when(mockErrorResponse.code()).thenReturn("InvalidAccessKeyId");

        final ErrorResponseException unauthorizedException = Mockito.mock(ErrorResponseException.class);
        Mockito.when(unauthorizedException.errorResponse()).thenReturn(mockErrorResponse);
        Mockito.when(mockResult.get()).thenThrow(unauthorizedException);

        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.deleteArtifacts(jobId));

    }

    @Test
    public void testDelete_GetArtifacts_NetworkFailure_Error() throws Exception {
        final UUID jobId = UUID.randomUUID();
        final ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().recursive(true).bucket(bucketName).prefix(jobId.toString()).build();
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();

        final Iterable<Result<Item>> mockIterable = Mockito.mock(Iterable.class);
        final Iterator<Result<Item>> mockIterator = Mockito.mock(Iterator.class);
        final Result<Item> mockResult = Mockito.mock(Result.class);


        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(true);
        Mockito.when(minioClient.listObjects(listObjectsArgs)).thenReturn(mockIterable);
        Mockito.when(mockIterable.iterator()).thenReturn(mockIterator);
        Mockito.when(mockIterator.hasNext()).thenReturn(true);
        Mockito.when(mockIterator.next()).thenReturn(mockResult);

        final ConnectException connectException = Mockito.mock(ConnectException.class);

        Mockito.when(mockResult.get()).thenThrow(connectException);

        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.deleteArtifacts(jobId));
    }


    @Test
    public void testRemoveArtifacts_Success() throws Exception {
        final UUID jobId = UUID.randomUUID();
        final Result<Item> mockResult = Mockito.mock(Result.class);
        final Item mockItem = Mockito.mock(Item.class);
        final ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().recursive(true).bucket(bucketName).prefix(jobId.toString()).build();
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();


        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(true);
        Mockito.when(minioClient.listObjects(listObjectsArgs)).thenReturn(Collections.singletonList(mockResult));
        Mockito.when(mockResult.get()).thenReturn(mockItem);
        Mockito.when(mockItem.objectName()).thenReturn("someObjectName");

        Assertions.assertDoesNotThrow(()
            -> artifactStorageService.deleteArtifacts(jobId));
    }

    @Test
    public void testRemoveArtifacts_PartialDeletion_Error() throws Exception {
        final UUID jobId = UUID.randomUUID();
        final Result<Item> mockResult1 = Mockito.mock(Result.class);
        final Result<Item> mockResult2 = Mockito.mock(Result.class);
        final Item mockItem1 = Mockito.mock(Item.class);
        final Item mockItem2 = Mockito.mock(Item.class);
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
        final ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().recursive(true).bucket(bucketName).prefix(jobId.toString()).build();

        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(true);
        Mockito.when(minioClient.listObjects(listObjectsArgs)).thenReturn(Arrays.asList(mockResult1, mockResult2));
        Mockito.when(mockResult1.get()).thenReturn(mockItem1);
        Mockito.when(mockResult2.get()).thenReturn(mockItem2);
        Mockito.when(mockItem1.objectName()).thenReturn("item1");
        Mockito.when(mockItem2.objectName()).thenReturn("item2");


        final RemoveObjectArgs removeObjectArgs1 = RemoveObjectArgs.builder().bucket(bucketName).object("item1").build();
        final RemoveObjectArgs removeObjectArgs2 = RemoveObjectArgs.builder().bucket(bucketName).object("item2").build();

        Mockito.doNothing().when(minioClient).removeObject(removeObjectArgs1);
        Mockito.doThrow(new ConnectException("Connection failed")).when(minioClient).removeObject(removeObjectArgs2);
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.deleteArtifacts(jobId));
    }

    @Test
    public void testRemoveArtifacts_AuthenticateFailure_Error() throws Exception {
        final UUID jobId = UUID.randomUUID();
        final Result<Item> mockResult1 = Mockito.mock(Result.class);
        final Result<Item> mockResult2 = Mockito.mock(Result.class);
        final Item mockItem1 = Mockito.mock(Item.class);
        final Item mockItem2 = Mockito.mock(Item.class);
        final BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
        final ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().recursive(true).bucket(bucketName).prefix(jobId.toString()).build();

        Mockito.when(minioClient.bucketExists(bucketExistsArgs)).thenReturn(true);
        Mockito.when(minioClient.listObjects(listObjectsArgs)).thenReturn(Arrays.asList(mockResult1, mockResult2));
        Mockito.when(mockResult1.get()).thenReturn(mockItem1);
        Mockito.when(mockResult2.get()).thenReturn(mockItem2);
        Mockito.when(mockItem1.objectName()).thenReturn("item1");
        Mockito.when(mockItem2.objectName()).thenReturn("item2");

        final ErrorResponse mockErrorResponse = Mockito.mock(ErrorResponse.class);
        Mockito.when(mockErrorResponse.code()).thenReturn("InvalidAccessKeyId");

        final ErrorResponseException unauthorizedException = Mockito.mock(ErrorResponseException.class);
        Mockito.when(unauthorizedException.errorResponse()).thenReturn(mockErrorResponse);

        final RemoveObjectArgs removeObjectArgs1 = RemoveObjectArgs.builder().bucket(bucketName).object("item1").build();
        final RemoveObjectArgs removeObjectArgs2 = RemoveObjectArgs.builder().bucket(bucketName).object("item2").build();

        Mockito.doNothing().when(minioClient).removeObject(removeObjectArgs1);
        Mockito.doThrow(unauthorizedException).when(minioClient).removeObject(removeObjectArgs2);
        Assertions.assertThrows(OnboardingJobException.class, ()
            -> artifactStorageService.deleteArtifacts(jobId));
    }


    private Artifact createArtifact(final String id, final boolean shouldCreateFile) {
        final Artifact artifact = new Artifact();
        final String randomUUID = UUID.randomUUID().toString().substring(0, 4);
        artifact.setName("eric-oss-5gcnr" + randomUUID + "eric-oss-app-onboarding-eric-data-object-storage-mn-secret.yaml");
        artifact.setLocation(artifact.getName());
        artifact.setType("IMAGE");

        if (shouldCreateFile) {
            final Path location = Path.of(tempFolderLocation, id, artifact.getLocation());
            try {
                FileUtils.createNewFileInFolder(location, "new file");
            } catch (IOException e) {
                Assertions.fail("Failed to create new file: " + e.getMessage());
            }
        }

        return artifact;
    }

    private List<Artifact> createArtifactList(final String id, final boolean shouldCreateFile) {
        final Artifact artifact = createArtifact(id, shouldCreateFile);
        return new ArrayList<>(Collections.singletonList(artifact));
    }

    private OnboardingJobEntity mockOnboardingJobEntity() {
        final OnboardingJobEntity jobEntity = new OnboardingJobEntity();
        jobEntity.setId(UUID.randomUUID());
        jobEntity.setPackageVersion("1.0.0");
        jobEntity.setVendor("Ericsson");
        jobEntity.setType("rApp");
        jobEntity.setFileName("fake.csar");
        jobEntity.setPackageSize("1000 MiB");
        jobEntity.setStatus(OnboardingJobStatus.PARSED);
        jobEntity.setStartTimestamp(Timestamp.from(Instant.now()));
        jobEntity.setOnboardingEventEntities(new HashSet<>());
        return jobEntity;
    }

    private Component createAppComponent(final String id, final boolean shouldCreateFile) {
        final Component component = new Component();
        component.setArtifacts(createArtifactList(id, shouldCreateFile));
        return component;
    }

    private CreateAppRequest mockCreateAppRequest(final String jobId, final boolean shouldCreateFile) {
        this.jobId = UUID.fromString(jobId);
        final CreateAppRequest request = new CreateAppRequest();
        request.setName("fake");
        request.setType("rApp");
        final Component component1 = createAppComponent(jobId, shouldCreateFile);
        final Component component2 = createAppComponent(jobId, shouldCreateFile);
        request.setComponents(new ArrayList<>(Arrays.asList(component1, component2)));
        return request;
    }
}
