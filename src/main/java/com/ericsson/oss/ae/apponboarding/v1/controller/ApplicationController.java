/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.ae.apponboarding.v1.controller;

import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V1_ONBOARD_AN_APP;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V1_DELETING_APP;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V1_DISABLE_APP;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V1_ENABLE_APP;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V1_DELETE_APP_BY_ID;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogConstants.ADDITIONAL_MESSAGE_KEY;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_MODE_ENABLED;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_MODE_DISABLED;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_STATUS_DELETING;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.API_V1_APPS;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.API_ARTIFACT;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FILE;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.SUBJECT_NOT_AVAILABLE;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.TEMP_FILE_ATT_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.ericsson.oss.ae.api.v1.model.AppOnboardingPutRequestDto;
import com.ericsson.oss.ae.apponboarding.common.consts.utils.StringUtils;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Artifact;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingException;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.NoSuchElementFoundException;
import com.ericsson.oss.ae.apponboarding.v1.service.ApplicationService;
import com.ericsson.oss.ae.apponboarding.v1.service.ArtifactRegistryService;
import com.ericsson.oss.ae.apponboarding.v1.service.ArtifactService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = API_V1_APPS, produces = MediaType.APPLICATION_JSON_VALUE)
public class ApplicationController {
    @Autowired
    private ArtifactRepository artifactRepository;
    @Autowired
    private ArtifactRegistryService artifactRegistryService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ArtifactService artifactService;
    @Autowired
    private NativeWebRequest nativeWebRequest;

    private Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(nativeWebRequest);
    }

    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    @GetMapping
    public ResponseEntity<List<Application>> getAll(@QueryParam(value = "q") final String q,
                                                    @QueryParam(value = "sort") final String sort,
                                                    @QueryParam(value = "offset") final String offset,
                                                    @QueryParam(value = "limit") final String limit) {

        Instant startTime = Instant.now();
        logger.info("getAll() onboarded apps has been called");
        ResponseEntity<List<Application>> response = new ResponseEntity<>(applicationService.findAll(q, sort, offset, limit), HttpStatus.OK);
        logger.info("getAll() apps - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
        return response;

    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> get(@PathVariable final Long id) {
        Instant startTime = Instant.now();
        logger.info("get() app by id: {}", id);
        ResponseEntity<Application> response = new ResponseEntity<>(applicationService.findById(id)
            .orElseThrow(() -> new NoSuchElementFoundException(1500, StringUtils.appendWithSpace("Application not found, ID:", id))), HttpStatus.OK);
        logger.info("get() app by id - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
        return response;
    }

    @GetMapping("/{id}/" + API_ARTIFACT)
    public ResponseEntity<Set<Artifact>> getArtifacts(@PathVariable final Long id) {
        Instant startTime = Instant.now();
        logger.info("getArtifacts() by app id: {}", id);
        ResponseEntity<Set<Artifact>> response = new ResponseEntity<>(applicationService.getArtifacts(id), HttpStatus.OK);
        logger.info("getArtifacts() by app id - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
        return response;
    }

    @GetMapping("/{id}/" + API_ARTIFACT + "/{artifactId}")
    public ResponseEntity<Artifact> getArtifactsById(@PathVariable final Long id, @PathVariable final Long artifactId) {
        Instant startTime = Instant.now();
        logger.info("getArtifactsById() has been called with artifact id: {} and app id: {}", artifactId, id);
        final Optional<Artifact> artifact = artifactService.getArtifactsById(id, artifactId);
        if (artifact.isEmpty()) {
            logger.error("getArtifactsById() couldn't find any artifact with in the application");
            throw new AppOnboardingException(2500,
                StringUtils.appendWithSpace("Could not find Artifact with id:", artifactId, " in app with id: ", id), HttpStatus.NOT_FOUND);

        } else {
            ResponseEntity<Artifact> response = new ResponseEntity<>(artifact.get(), HttpStatus.OK);
            logger.info("getArtifactsById() with application id - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
            return response;
        }
    }

    @GetMapping("/{id}/" + API_ARTIFACT + "/{artifactId}/" + FILE)
    public ResponseEntity<ByteArrayResource> getArtifactsFile(@PathVariable final Long id, @PathVariable final Long artifactId,
                                                              final HttpServletRequest request) throws IOException {
        Instant startTime = Instant.now();
        logger.info("getArtifactsFile() by artifact id: {} and app id: {} ", artifactId, id);
        final String downloadFileName = artifactRegistryService.downloadArtifact(id, artifactId);
        final File file = new File(downloadFileName);
        final Path path = Paths.get(file.getAbsolutePath());
        final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
        request.setAttribute(TEMP_FILE_ATT_NAME, downloadFileName);
        ResponseEntity<ByteArrayResource> response = ResponseEntity.ok().contentLength(file.length()).contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
        logger.info("getArtifactsFile() by artifact id - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
        return response;

    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Application> onboardingApps(@RequestPart final MultipartFile file) {
        Instant startTime = Instant.now();
        final String fileName = file.getOriginalFilename();
        logger.info("onboardingApps() request made to onboard app package: {} with the size of {}", fileName, file.getSize());
        setAdditionalAuditLogMessage(String.format(V1_ONBOARD_AN_APP, SUBJECT_NOT_AVAILABLE, fileName));
        if (fileName != null && fileName.toLowerCase(Locale.getDefault()).endsWith(".csar")) {
            try {
                final Application application = applicationService.onboardApp(file);
                setAdditionalAuditLogMessage(String.format(V1_ONBOARD_AN_APP,
                        Optional.ofNullable(application).map(Application::getId).orElse(null), fileName));
                return new ResponseEntity(application, HttpStatus.ACCEPTED);
            } catch (final TransactionException | DataAccessException databaseException) {
                logger.error("onboardingApps() failed to onboard app package: {} due to database exception {}", fileName,
                        databaseException.getMessage());
                throw databaseException;
            } catch (final Exception e) {
                logger.error("onboardingApps() failed to onboard app package: {} due to exception {}", fileName, e.getMessage());
                return new ResponseEntity(new RuntimeException(e), HttpStatus.BAD_REQUEST);
            } finally {
                logger.info("onboardingApps() with package - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
            }

        } else {
            logger.error("onboardingApps() failed to onboard app package: {} as file extension is not .csar ", fileName);
            throw new AppOnboardingException("File extension is invalid, please upload a file with .csar extension.", HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Application> updateApp(@PathVariable final Long id, @RequestBody final AppOnboardingPutRequestDto appRequest) {
        Instant startTime = Instant.now();
        setAdditionalLogMessageForUpdate(id, appRequest);
        logger.info("updateApp() request made to update App with id: {}", id);
        ResponseEntity<Application> response = new ResponseEntity<>(applicationService.updateApplication(id, appRequest), HttpStatus.OK);
        logger.info("updateApp() with app id - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
        return response;
    }

    @Hidden
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Application> deleteApp(@PathVariable final Long id) {
        Instant startTime = Instant.now();
        logger.info("deleteApp() request made to delete App with id: {}", id);
        setAdditionalAuditLogMessage(String.format(V1_DELETE_APP_BY_ID, id));
        applicationService.deleteApplication(id);
        logger.info("deleteApp() with app id - response time: {}ms", Duration.between(startTime, Instant.now()).toMillis());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void setAdditionalAuditLogMessage(final String additionalMessage) {
        getRequest().ifPresent(request -> request.setAttribute(ADDITIONAL_MESSAGE_KEY, additionalMessage, NativeWebRequest.SCOPE_REQUEST));
    }

    private void setAdditionalLogMessageForUpdate(final Long id, final AppOnboardingPutRequestDto appRequest){
        if (appRequest.getStatus() == null) {
            if (appRequest.getMode().getValue().equals(APP_MODE_ENABLED)) {
                setAdditionalAuditLogMessage(String.format(V1_ENABLE_APP, id));
            }
            if (appRequest.getMode().getValue().equals(APP_MODE_DISABLED)) {
                setAdditionalAuditLogMessage(String.format(V1_DISABLE_APP, id));
            }
        } else if (appRequest.getStatus().getValue().equals(APP_STATUS_DELETING)) {
            setAdditionalAuditLogMessage(String.format(V1_DELETING_APP, id));
        }
    }

}
