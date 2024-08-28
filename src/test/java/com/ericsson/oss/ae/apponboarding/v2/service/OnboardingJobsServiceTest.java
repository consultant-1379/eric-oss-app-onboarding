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

package com.ericsson.oss.ae.apponboarding.v2.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobItems;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.RepositoryTestUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
public class OnboardingJobsServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(OnboardingJobsServiceTest.class);
    @Autowired
    private OnboardingJobRepository onboardingJobRepository;
    @Autowired
    private OnboardingJobsService onboardingJobService;

    private final Map<String, String> queryOnFileName = new HashMap<>();

    @BeforeAll
    public void setup(){
        queryOnFileName.put("fileName", "hello");
    }
    @AfterEach
    public void cleanUp() {
        onboardingJobRepository.deleteAll();
    }

    @Test
    public void testGetOnboardingJobAllNull_ok() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(null, null, null, null);
        // Then
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_ok() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryOnFileName, "vendor", "10", "1");
        // Then
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_ok_paramSortNull() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryOnFileName, null, "10", "1");
        // Then
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_ok_paramSortEmpty() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryOnFileName, "", "10", "1");
        // Then
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_ok_paramOffsetEmpty() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryOnFileName, "", "", "");
        // Then
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_ok_paramOffsetNullButNotLimit() throws Exception {
        // Given multiple jobs are onboarded
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));

        // When getOnboardingAllJobsList is called
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryOnFileName, "", null, "5");

        // Then Multiple jobs are found
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_ok_paramOffsetEmptyButNotLimit() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryOnFileName, "", "", "5");
        // Then
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_ok_paramOffsetNull() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryOnFileName, "", null, null);
        // Then
        assertMultipleOnboardedJobsSuccess(retrievedItems.getItems());
    }

    @Test
    public void testGetOnboardingAllJobsList_throwsException() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final Map<String, String> queryValueMissing = new HashMap<>();
        queryValueMissing.put("filename", "");
        // Then
        assertThrows(OnboardingJobException.class, () -> onboardingJobService.getOnboardingAllJobsList(queryValueMissing, "", "", ""));
    }

    @Test
    public void getAllOnboardingJobsSortByFileName() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJobForQueryAndSort(OnboardingJobStatus.FAILED));
        // When
        final OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(null, "fileName", null, null);
        final List<OnboardingJob> orderedList = new ArrayList<>(List.copyOf(retrievedItems.getItems()));
        final List<OnboardingJob> sortedListID = sortBy(orderedList, "fileName");
        //Then
        Assertions.assertEquals(retrievedItems.getItems(), sortedListID);
    }

    @Test
    public void getAllOnboardingJobsSortByPackageVersion() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJobForQueryAndSort(OnboardingJobStatus.FAILED));
        // When
        OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(null, "packageVersion", null, "10");
        List<OnboardingJob> orderedList = new ArrayList<>(List.copyOf(retrievedItems.getItems()));
        List<OnboardingJob> sortedListID = sortBy(orderedList, "packageVersion");
        //Then
        Assertions.assertEquals(retrievedItems.getItems(), sortedListID);
    }

    @Test
    public void getAllOnboardingJobsSetOffsetButNoLimit() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final Map<String, String> queryValueMissing = new HashMap<>();
        queryValueMissing.put("filename", "");
        // Then
        assertThrows(OnboardingJobException.class, () -> onboardingJobService.getOnboardingAllJobsList(queryValueMissing, "", "10", ""));
    }

    @Test
    public void getAllOnboardingJobsSetOffsetButLimitNull() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final Map<String, String> queryValueMissing = new HashMap<>();
        queryValueMissing.put("filename", "");
        // Then
        assertThrows(OnboardingJobException.class, () -> onboardingJobService.getOnboardingAllJobsList(queryValueMissing, "", "10", null));
    }

    @Test
    public void getAllOnboardingJobsSetOffsetNullLimitHasValue() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        // When
        final Map<String, String> queryValueMissing = new HashMap<>();
        queryValueMissing.put("filename", "");
        // Then
        assertThrows(OnboardingJobException.class, () -> onboardingJobService.getOnboardingAllJobsList(queryValueMissing, "", null, "1"));
    }

    @Test
    public void getAllOnboardingJobsQueryFilterWithID() throws Exception {
        // Given
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.UNPACKED));
        onboardingJobRepository.save(RepositoryTestUtils.dummyOnboardingJob(OnboardingJobStatus.FAILED));
        //When
        OnboardingJobItems allItems = onboardingJobService.getOnboardingAllJobsList(null, null, null, null);

        UUID findById = allItems.getItems().get(0).getId();
        List<OnboardingJob> specificAppToFindBy = new ArrayList<>();
        specificAppToFindBy.add(allItems.getItems().get(0));
        final Map<String, String> queryById = new HashMap<>();
        queryById.put("id", findById.toString());

        OnboardingJobItems retrievedItems = onboardingJobService.getOnboardingAllJobsList(queryById, null, null, null);
        //Then
        Assertions.assertEquals(specificAppToFindBy, retrievedItems.getItems());
    }

    private List<OnboardingJob> sortBy(List<OnboardingJob> unorderedList, String orderBy) {
        Comparator<OnboardingJob> idComparator = Comparator.comparing(OnboardingJob::getId);

        Comparator<OnboardingJob> nameComparator = Comparator.comparing(OnboardingJob::getFileName);

        Comparator<OnboardingJob> vendorComparator = (onboardingJob1, onboardingJob2) -> (onboardingJob1.getVendor() == null || onboardingJob2.getVendor() == null ?
            0 :
            onboardingJob1.getVendor().compareTo(onboardingJob2.getVendor()));

        Comparator<OnboardingJob> packageSizeComparator = Comparator.comparing(OnboardingJob::getPackageSize);

        Comparator<OnboardingJob> typeComparator = Comparator.comparing(OnboardingJob::getType);

        Comparator<OnboardingJob> statusComparator = (onboardingJob1, onboardingJob2) -> (onboardingJob1.getStatus() == null || onboardingJob2.getStatus() == null ?
            0 :
            onboardingJob1.getStatus().getValue().compareTo(onboardingJob2.getStatus().getValue()));

        Comparator<OnboardingJob> appIdComparator = (onboardingJob1, onboardingJob2) -> (onboardingJob1.getApp().getId() == null || onboardingJob2.getApp().getId() == null ?
            0 :
            onboardingJob1.getApp().getId().compareTo(onboardingJob2.getApp().getId()));

        Comparator<OnboardingJob> versionComparator = (onboardingJob1, onboardingJob2) -> (onboardingJob1.getPackageVersion() == null || onboardingJob2.getPackageVersion() == null ?
            0 :
            onboardingJob1.getPackageVersion().compareTo(onboardingJob2.getPackageVersion()));

        switch (orderBy) {
            case "fileName":
                unorderedList.sort(nameComparator);
                return unorderedList;
            case "id":
                unorderedList.sort(idComparator);
                return unorderedList;
            case "vendor":
                unorderedList.sort(vendorComparator);
                return unorderedList;
            case "packageVersion":
                unorderedList.sort(versionComparator);
                return unorderedList;
            case "packageSize":
                unorderedList.sort(packageSizeComparator);
                return unorderedList;
            case "type":
                unorderedList.sort(typeComparator);
                return unorderedList;
            case "status":
                unorderedList.sort(statusComparator);
                return unorderedList;
            case "appId":
                unorderedList.sort(appIdComparator);
                return unorderedList;
            default:
                return unorderedList;
        }
    }

    private static void assertMultipleOnboardedJobsSuccess(final List<OnboardingJob> retrievedAllJobs) {
        Assertions.assertNotNull(retrievedAllJobs);
        if (retrievedAllJobs.size() >= 2) {
            final OnboardingJob job1 = retrievedAllJobs.get(0);
            final OnboardingJob job2 = retrievedAllJobs.get(1);
            assertTrue(job1.getOnboardStartedAt().compareTo(job2.getOnboardEndedAt()) <= 0);
        }
    }
}
