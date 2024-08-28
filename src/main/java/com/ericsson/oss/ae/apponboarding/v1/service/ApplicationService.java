/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

package com.ericsson.oss.ae.apponboarding.v1.service;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.ae.api.v1.model.AppOnboardingPutRequestDto;
import com.ericsson.oss.ae.apponboarding.common.validation.FileNameValidator;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.common.consts.utils.StringUtils;
import com.ericsson.oss.ae.apponboarding.common.model.entity.FilterQuery;
import com.ericsson.oss.ae.apponboarding.common.validation.FilterQueryValidator;
import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationMode;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Artifact;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.SearchSpecification;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingException;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingValidationException;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.NoSuchElementFoundException;
import com.ericsson.oss.ae.apponboarding.v1.service.jobs.JobsService;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ApplicationService {

    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private JobsService jobsService;
    @Autowired private ArtifactService artifactService;
    @Autowired private FilterQueryValidator filterQueryValidator;
    @Autowired private FileNameValidator fileNameValidator;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    @Value("${deployment.docker.pythonJobImageName}") private String pythonJobImageName;

    @PersistenceContext private EntityManager entityManager;

    public Application onboardApp(final MultipartFile file) throws IOException {
        validateUploadedFileName(file);
        saveFileToLocalFilesystem(file);
        final Application application = createApplication(file);
        logger.info("onboardApp() saving App with name: {} to db.", application.getName());
        final Application savedApp = applicationRepository.save(application);

        startAppPackageJob(savedApp);
        return savedApp;
    }

    public Optional<Application> findById(Long id) {
        return applicationRepository.findById(id);
    }

    public Set<Artifact> getArtifacts(Long id) {
        Optional<Application> application = applicationRepository.findById(id);

        if (application.isPresent()) {
            return application.get().getArtifacts();
        } else {
            logger.error("getArtifacts() no artifact found with in the application");
            throw new NoSuchElementFoundException(1500, StringUtils.appendWithSpace("Application not found, ID:", id));
        }
    }

    public void saveApplication(Application app) {
        applicationRepository.save(app);
    }

    public Application findApplicationEvents(Long appId) {
        EntityGraph entityGraph = entityManager.getEntityGraph("application-events-entity-graph");
        Map<String, Object> properties = new HashMap<>();
        properties.put("javax.persistence.fetchgraph", entityGraph);
        logger.info("findApplicationEvents() retrieving events for App with ID: {}", appId);
        return entityManager.find(Application.class, appId, properties);
    }

    public Application updateApplication(final Long id, final AppOnboardingPutRequestDto appDto) {
        final Application appToUpdate = this.findById(id)
            .orElseThrow(() -> new NoSuchElementFoundException(1500, StringUtils.appendWithSpace("Application not found, ID:", id)));

        if (appDto.getMode() != null) {
            setApplicationMode(appToUpdate, appDto);
        }

        if (appDto.getStatus() != null) {
            setApplicationStatus(appToUpdate, appDto);
        }

        saveApplication(appToUpdate);

        return this.findById(id)
            .orElseThrow(() -> new NoSuchElementFoundException(1500, StringUtils.appendWithSpace("Application not found, ID:", id)));
    }

    public List<Application> findAllUploadedApplications() {
        return applicationRepository.findByStatusAndOnboardedDateBefore(
                ApplicationStatus.UPLOADED, new Date(System.currentTimeMillis() - (Consts.ONBOARDED_TIME_DIFFERENCE)));
    }

    public List<Application> findAppsWithNameAndVersionAndStatus(final String name, final String version, final ApplicationStatus status) {
        if(org.apache.commons.lang3.StringUtils.isNotBlank(name) && org.apache.commons.lang3.StringUtils.isNotBlank(version)) {
            return applicationRepository.findByNameAndVersionAndStatus(name, version, status);
        }
        return new ArrayList<>();
    }

    public void setApplicationToDeleting(final Application appToUpdate) {
        appToUpdate.setStatus(ApplicationStatus.DELETING);
        saveApplication(appToUpdate);
    }

    @Transactional
    public void deleteApplication(final Long id) {
        logger.debug("deleteApplication() retrieving App from db with id: {}", id);
        final Application appToDelete = this.findById(id)
            .orElseThrow(() -> new NoSuchElementFoundException(1500, StringUtils.appendWithSpace("Application not found, ID:", id)));
        logger.info("deleteApplication() App with Id: {} found in the db", id);

        checkApplicationMode(appToDelete, EnumSet.of(ApplicationMode.DISABLED));
        logger.info("deleteApplication() App (id: {}) mode is valid for deletion", id);
        checkApplicationStatus(appToDelete, EnumSet.of(ApplicationStatus.DELETING));
        logger.info("deleteApplication() App (id: {}) status is valid for deletion", id);
        deleteApplicationArtifacts(appToDelete);
        logger.info("deleteApplication() deleted all artifacts associated with App (id: {})", id);
        deleteApplicationClearDB(appToDelete);
        logger.info("deleteApplication() App with Id: {} deleted from the db", id);
    }

    private void setApplicationMode(Application appToUpdate, AppOnboardingPutRequestDto appDto) {
        if (isModeDifferent(appToUpdate, appDto)) {
            checkApplicationStatus(appToUpdate, EnumSet.of(ApplicationStatus.ONBOARDED));
            appToUpdate.setMode(ApplicationMode.valueOf(appDto.getMode().toString()));
        }
    }

    private void setApplicationStatus(Application appToUpdate, AppOnboardingPutRequestDto appDto) {
        if (appDto.getStatus().toString().equals(ApplicationStatus.DELETING.toString())) {
            checkApplicationStatus(appToUpdate, EnumSet.of(ApplicationStatus.ONBOARDED, ApplicationStatus.FAILED));
            checkApplicationMode(appToUpdate, EnumSet.of(ApplicationMode.DISABLED));
            appToUpdate.setStatus(ApplicationStatus.DELETING);
        } else {
            logger.error("setApplicationStatus() failed to update App with id: {} due to an invalid status of: {}", appToUpdate.getId(),
                appToUpdate.getStatus());
            throw new AppOnboardingException(
                StringUtils.appendWithSpace("App with id:", appToUpdate.getId(), "status cannot be set to an invalid status"),
                HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Set<Long>> getArtifactLocations(Set<Artifact> artifactsToDelete) {
        logger.debug("getArtifactLocations() - retrieving artifacts by location");
        List<String> locationsToDelete = artifactsToDelete.stream().map(Artifact::getLocation)
            .filter(location -> location != null && !location.isEmpty()).collect(Collectors.toList());
        List<Artifact> allArtifacts = artifactService.findArtifactsByLocation(locationsToDelete);
        Map<String, Set<Long>> artifactsLocationsWithAppsIds = new HashMap<>();
        for (Artifact artifact : allArtifacts) {
            artifactsLocationsWithAppsIds.computeIfAbsent(artifact.getLocation(), k -> new HashSet<>());
            artifactsLocationsWithAppsIds.get(artifact.getLocation()).add(artifact.getApplication().getId());
        }
        logger.info("getArtifactLocations() collected all artifacts in the db matching all artifacts' locations in the current App");
        return artifactsLocationsWithAppsIds;
    }

    private void deleteApplicationArtifacts(Application appToDelete) {
        Map<String, Set<Long>> artifactsLocationsWithAppsIds = getArtifactLocations(appToDelete.getArtifacts());
        for (Artifact artifact : appToDelete.getArtifacts()) {
            if (isArtifactLocationUnique(artifactsLocationsWithAppsIds, artifact)) {
                try {
                    artifactService.deleteArtifact(artifact);
                } catch (AppOnboardingException aoe) {
                    if (aoe.getHttpStatus() == HttpStatus.NOT_FOUND) {
                        logger.error("deleteApplicationArtifacts() - artifact {} not found in the registry, continue to verify db",
                            artifact.getName());
                    } else {
                        throw aoe;
                    }
                }
                artifactsLocationsWithAppsIds.get(artifact.getLocation()).clear();
            }
        }
    }

    private void deleteApplicationClearDB(Application appToDelete) {
        applicationRepository.delete(appToDelete);
    }

    private void checkApplicationMode(Application app, Set<ApplicationMode> validModes) {
        if (!validModes.contains(app.getMode())) {
            logger.error("checkApplicationMode() - App mode with id {} is not valid. Current App mode: {}", app.getId(), app.getMode());
            throw new AppOnboardingException(
                StringUtils.appendWithSpace("App mode with id: ", app.getId(), " is not valid, current mode is ", app.getMode()),
                HttpStatus.BAD_REQUEST);
        }
    }

    private void checkApplicationStatus(Application app, Set<ApplicationStatus> validStatus) {
        if (!validStatus.contains(app.getStatus())) {
            logger.error("checkApplicationStatus() - App status with id {} is not valid. Current App status: {}", app.getId(), app.getStatus());
            throw new AppOnboardingException(
                StringUtils.appendWithSpace("App status with id: ", app.getId(), " is not valid, current status is ", app.getStatus()),
                HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isArtifactLocationUnique(Map<String, Set<Long>> artifactsLocationsWithAppsIds, Artifact artifact) {
        if (artifact.getLocation() != null && !artifact.getLocation().isEmpty()) {
            logger.debug("isArtifactLocationUnique() -  artifact is Unique");
            return artifactsLocationsWithAppsIds.get(artifact.getLocation()).size() == 1;
        } else {
            logger.debug("isArtifactLocationUnique() -  artifact is not Unique");
            return false;
        }
    }

    private boolean isModeDifferent(Application appToUpdate, AppOnboardingPutRequestDto appDto) {
        if (appToUpdate.getMode().toString().equals(appDto.getMode().toString())) {
            logger.debug("isModeDifferent() App with name: {} and version: {} already set to mode: {}", appToUpdate.getName(),
                appToUpdate.getVersion(), appToUpdate.getMode());
            return false;
        } else {
            logger.debug("isModeDifferent() updated App mode to: {} with name: {} and version: {}", appDto.getMode(), appToUpdate.getName(),
                appToUpdate.getVersion());
            return true;
        }
    }

    private void saveFileToLocalFilesystem(MultipartFile file) throws IOException {
        final Path destinationFile = Paths.get("/tmp/" + file.getOriginalFilename());
        final byte[] bufferedbytes = new byte[1024];
        int count = 0;
        final BufferedInputStream fileInputStream = new BufferedInputStream(file.getInputStream());
        logger.debug("saveFileToLocalFilesystem() writing to file {}", destinationFile);
        try (final FileOutputStream outStream = new FileOutputStream(destinationFile.toFile())) {

            while ((count = fileInputStream.read(bufferedbytes)) != -1) {
                outStream.write(bufferedbytes, 0, count);
            }
        }
    }

    private Application createApplication(MultipartFile file) {
        final Application application = new Application();
        application.setName(file.getOriginalFilename());
        application.setSize(convertToMBs(file));
        application.setType("APP");
        application.setUsername("Unknown");
        application.setVendor("Unknown");
        application.setVersion("1.1.1");
        application.setStatus(ApplicationStatus.UPLOADED);
        application.setOnboardedDate(new Date(System.currentTimeMillis()));
        application.setMode(ApplicationMode.DISABLED);
        return application;
    }

    private static String convertToMBs(final MultipartFile file) {
        return BigDecimal.valueOf(file.getSize())
                .setScale(4, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(Consts.ONE_MB_IN_BYTES), RoundingMode.HALF_UP) + Consts.MEGABYTE_SYMBOL;
    }

    private void startAppPackageJob(final Application savedApp) {
        final List<String> args = new ArrayList<>();
        String pitag = pythonJobImageName.split(":")[1];
        args.add("python3 main-job.py " + savedApp.getId() + " '" + savedApp.getName() + "' " + pitag);
        logger.debug("startAppPackageJob() creating jobs to process App package: {}", savedApp.getName());
        jobsService.createJobs(args, pythonJobImageName, savedApp.getId());
    }

    public List<Application> findAll(String queryParam, String sort, String offset, String limit) {

        FilterQuery filter = new FilterQuery(queryParam, sort, limit, offset);

        Errors errors = this.getErrorsObj(filter.getClass());
        ValidationUtils.invokeValidator(filterQueryValidator, filter, errors);

        if (errors.hasErrors()) {
            throw new AppOnboardingValidationException("Validation failed", HttpStatus.BAD_REQUEST, errors);
        }

        SearchSpecification searchSpec = null;
        Page<Application> page;
        Pageable pageable = null;
        Sort sortCriteria = buildSortCriteria(FilterQuery.VALID_FILTER_FIELDS.ID.name().toLowerCase(Locale.ENGLISH));

        if (filter.getSort() != null && !filter.getSort().isEmpty()) {
            sortCriteria = buildSortCriteria(filter.getSort());
        }

        if (filter.getOffset() != null && !filter.getOffset().isEmpty()) {
            pageable = PageRequest.of(this.getIntValue(filter.getOffset()), this.getIntValue(filter.getLimit()), sortCriteria);
        } else if (filter.getLimit() != null && !filter.getLimit().isEmpty()) {
            pageable = PageRequest.of(Consts.DEFAULT_OFFSET, this.getIntValue(filter.getLimit()), sortCriteria);
        }

        if (filter.getQueryParam() != null && !filter.getQueryParam().isEmpty()) {
            searchSpec = this.parse(filter.getQueryParam());
        }

        if (pageable != null) {
            logger.info("findAll() with queryParam: {} and pageable: {}", filter.getQueryParam(), pageable);
            page = applicationRepository.findAll(Specification.where(searchSpec), pageable);
            return page.getContent();
        } else if (searchSpec != null) {
            logger.info("findAll() with queryParam: {} and sort: {}", filter.getQueryParam(), pageable);
            return applicationRepository.findAll(Specification.where(searchSpec), sortCriteria);
        } else if (filter.getSort() != null && !filter.getSort().isEmpty()) {
            logger.info("findAll() with only sort: {}", sortCriteria);
            return applicationRepository.findAll(sortCriteria);
        } else {
            logger.info("findAll() with no filter queries");
            return applicationRepository.findAll();
        }
    }

    private SearchSpecification parse(String value) {
        String[] keyValue = value.split(Consts.QUERY_PARAM_KEY_VALUE_SEPARATOR);
        return new SearchSpecification(keyValue[0], keyValue[1]);
    }

    private Sort buildSortCriteria(String sortBy) {
        return Sort.by(Sort.Order.asc(sortBy));
    }

    private Errors getErrorsObj(final Class<?> clazz) {
        return new MapBindingResult(new HashMap<>(), clazz.getSimpleName());
    }

    private int getIntValue(String val) {
        return Integer.parseInt(val);
    }

    private void validateUploadedFileName(final MultipartFile file){
        final Errors errors = this.getErrorsObj(file.getClass());
        ValidationUtils.invokeValidator(fileNameValidator, file, errors);
        if (errors.hasErrors()) {
            throw new AppOnboardingValidationException("Validation failed", HttpStatus.BAD_REQUEST, errors);
        }
    }
}