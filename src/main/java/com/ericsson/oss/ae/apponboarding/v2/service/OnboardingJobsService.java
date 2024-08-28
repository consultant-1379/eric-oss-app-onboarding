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

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.COMMA;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.EQUAL;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.SPACE;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DELETE_JOB_INVALID_STATE;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.ONBOARDING_JOB_ENTITY_NOT_FOUND;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.PLEASE_ONBOARD_VALID_CSAR;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.QUERY_MULTIPLE_PROPERTIES_NOT_ALLOWED;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.ae.apponboarding.api.v2.model.AppPackageResponse;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobItems;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobLink;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.common.model.entity.FilterQuery;
import com.ericsson.oss.ae.apponboarding.common.validation.FileNameValidator;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import com.ericsson.oss.ae.apponboarding.v2.persistence.repository.OnboardingJobRepository;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobEntity;
import com.ericsson.oss.ae.apponboarding.v2.persistence.entity.OnboardingJobSearchSpecification;
import com.ericsson.oss.ae.apponboarding.v2.service.model.mapper.OnboardingJobMapper;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.UrlGenerator;
import com.ericsson.oss.ae.apponboarding.v2.validation.FilterQueryValidatorV2;
import jakarta.transaction.TransactionalException;

@Service
public class OnboardingJobsService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingJobsService.class);
    private static final String APP_PACKAGE_VENDOR_ERICSSON = "Ericsson";
    private static final String APP_PACKAGE_TYPE_RAPP = "rApp";

    @Autowired private AppPackageProcessor appPackageProcessor;
    @Autowired private OnboardingJobMapper onboardingJobMapper;
    @Autowired private FilterQueryValidatorV2 filterQueryValidator;
    @Autowired private FileNameValidator fileNameValidator;
    @Autowired private OnboardingJobRepository onboardingJobRepository;
    @Autowired private FileService fileService;
    @Autowired private ArtifactStorageService artifactStorageService;
    @Autowired
    private UrlGenerator urlGenerator;

    @Value("${onboarding-job.tempFolderLocation}") private String tempFolderLocation;

    /**
     * Implement method body to handle the POST /app-packages logic.  Creates a new Onboarding Job
     * to handle the request.
     *
     * @param file
     *     Onboarding file
     * @return AppPackageResponse
     */
    public AppPackageResponse onboardAppPackage(@Nonnull final MultipartFile file) {
        logger.info("onboardAppPackage() service request received for csar {}", file.getOriginalFilename());

        validateUploadedFileName(file);

        final OnboardingJobEntity onboardingJobEntity = createOnboardingJob(file);

        appPackageProcessor.processAppPackage(onboardingJobEntity.getFileName(), onboardingJobEntity.getId());

        // App Package was accepted by the processor, send the response data back
        return new AppPackageResponse()
            .fileName(file.getOriginalFilename())
            .onboardingJob(
                getOnboardingJobLink(onboardingJobEntity.getId()));
    }

    private OnboardingJobEntity createOnboardingJob(final MultipartFile file) {

        fileService.saveFileToLocalFilesystem(file);
        try {
            final OnboardingJobEntity onboardingJobEntity = createOnboardingJobEntity(file);
            return onboardingJobRepository.save(onboardingJobEntity);
        } catch (final TransactionalException | DataAccessException exception) {
            fileService.removeFileFromLocalFilesystem(file.getOriginalFilename());
            throw exception;
        }

    }

    /**
     * Get all onboarding-job resources matching the provided query parameters.
     *
     * @param query
     *     query to use
     * @param sort
     *     how to sort the result
     * @param offset
     *     which offset to use
     * @param limit
     *     max limit
     * @return List of onboarding-jobs as per query and sort parameters
     */
    public OnboardingJobItems getOnboardingAllJobsList(final Map<String, String> query, final String sort, final String offset, final String limit) {
        logger.info("getOnboardingAllJobs() returning all onboarding-jobs details, with filtering options q{}, sort{}, offset{}, limit{}", query, sort,
            offset, limit);
        return onboardingJobMapper.toOnboardingJobList(this.queryOnboardingJobs(query, sort, offset, limit));
    }

    /**
     * Get onboarding-job by job ID.
     *
     * @param jobId
     *     The id of job to retrieve
     * @return OnboardingJob The retreved job
     */
    public OnboardingJob getOnboardingJobById(final UUID jobId) {
        logger.info("getOnboardingJobById() returning onboarding-jobs details, with a job ID of {}", jobId);
        return getJobById(jobId);
    }


    /**
     * Delete an onboarding-job resource for the give job id. If the job is in state ROLLBACK_FAILED then delete its artifacts from object store.
     *
     * @param jobId
     *     The onboarding job ID.
     */
    public void deleteOnboardingJob(final UUID jobId) {
        logger.info("deleteOnboardingJob() service request called for onboarding-job with id: {}", jobId);
        final Optional<OnboardingJobEntity> optionalOnboardingJob = onboardingJobRepository.findById(jobId);
        if (optionalOnboardingJob.isPresent()) {
            final OnboardingJobEntity onboardingJobEntity = optionalOnboardingJob.get();
            checkAndHandleRollBackFailedState(onboardingJobEntity);
            deleteJobFromDB(onboardingJobEntity);
        } else {
            final String detail = String.format(ONBOARDING_JOB_ENTITY_NOT_FOUND, jobId);
            throw new OnboardingJobException(HttpStatus.NOT_FOUND, detail);
        }
    }

    public void updateOnboardingJob(final OnboardingJobEntity updatedJob)
    {
        onboardingJobRepository.saveAndFlush(updatedJob);
    }

    public List<OnboardingJobEntity> getOnboardingJobs(final List<OnboardingJobStatus> statusList, final Timestamp startTime)
    {
        return onboardingJobRepository.findByStatusInAndStartTimestampBefore(statusList, startTime);
    }



    private void deleteJobFromDB(final OnboardingJobEntity onboardingJobEntity) {
        if (isStatusForDeletion(onboardingJobEntity.getStatus())) {
            logger.info("deleteJobFromDB() deleting onboarding-job {} from database", onboardingJobEntity.getId());
            onboardingJobRepository.delete(onboardingJobEntity);
        } else {
            final String detail = String.format(DELETE_JOB_INVALID_STATE, onboardingJobEntity.getStatus());
            logger.info("deleteOnboardingJob() cannot delete onboarding-job from the DB: {}", detail);
            throw new OnboardingJobException(HttpStatus.BAD_REQUEST, detail);
        }
    }

    private boolean isStatusForDeletion(final OnboardingJobStatus jobStatus) {
        return jobStatus == OnboardingJobStatus.ONBOARDED ||
            jobStatus == OnboardingJobStatus.FAILED || jobStatus == OnboardingJobStatus.ROLLBACK_FAILED;
    }

    private void checkAndHandleRollBackFailedState(final OnboardingJobEntity onboardingJobEntity) {
        if (onboardingJobEntity.getStatus() == OnboardingJobStatus.ROLLBACK_FAILED) {
            logger.debug("checkAndHandleRollBackFailed() : onboarding-Job status is ROLLBACK_FAILED. Attempting to delete artifacts from storage.");
            try {
                artifactStorageService.deleteArtifacts(onboardingJobEntity.getId());
            }
            catch(final OnboardingJobException e)
            {
                logger.error("checkAndHandleRollBackFailedState() Failed to delete artifact from object store for job: {}: ",onboardingJobEntity.getId(), e);
            }
        }
    }

    private OnboardingJobEntity createOnboardingJobEntity(final MultipartFile file) {
        return OnboardingJobEntity.builder()
                .fileName(file.getOriginalFilename())
                .type(APP_PACKAGE_TYPE_RAPP)
                .packageSize(convertToMebibytes(file))
                .vendor(APP_PACKAGE_VENDOR_ERICSSON)
                .status(OnboardingJobStatus.UPLOADED)
                .build();
    }

    private static String convertToMebibytes(final MultipartFile file) {
        return BigDecimal.valueOf(file.getSize())
            .divide(BigDecimal.valueOf(Consts.ONE_MIB_IN_BYTES))
            .setScale(4, RoundingMode.HALF_UP) + Consts.MEBIBYTE_SYMBOL;
    }

    /**
     * Gets all onboarding jobs with the filter query parameters set
     *
     * @return list of onboarded jobs
     */
    private List<OnboardingJobEntity> queryOnboardingJobs(final Map<String, String> queryParam, final String sort, final String offset, final String limit) {

        final FilterQuery filter = validateQueryParameters(queryParam, sort, offset, limit);

        OnboardingJobSearchSpecification searchSpec = null;
        Page<OnboardingJobEntity> page;
        Pageable pageable;

        Sort sortCriteria = filterQueryValidator.buildSortCriteria(FilterQuery.VALID_FILTER_FIELDS.ID.name().toLowerCase(Locale.ENGLISH));

        sortCriteria = filterQueryValidator.getSortCriteria(filter, sortCriteria);

        pageable = filterQueryValidator.getPageable(filter, sortCriteria);

        if (!Optional.ofNullable(filter.getQueryParam()).orElse("").isEmpty()) {
            searchSpec = this.parse(filter.getQueryParam());
        }

        if (pageable != null) {
            logger.info("findAll() with queryParam: {} and pageable: {}", filter.getQueryParam(), pageable);
            page = onboardingJobRepository.findAll(Specification.where(searchSpec), pageable);
            return page.getContent();
        } else if (searchSpec != null) {
            logger.debug("findAll() with queryParam: {} and sort: {}", filter.getQueryParam(), sortCriteria);
            return onboardingJobRepository.findAll(Specification.where(searchSpec), sortCriteria);
        } else if (Optional.ofNullable(filter.getSort()).isPresent()) {
            logger.debug("findAll() with only sort: {}", sortCriteria);
            return onboardingJobRepository.findAll(sortCriteria);
        } else {
            logger.info("findAll() with no filter queries");
            return onboardingJobRepository.findAll();
        }
    }

    @NotNull
    private FilterQuery validateQueryParameters(final Map<String, String> queryParams, final String sort, final String offset, final String limit) {
        // To reduce ACM impacts after change to the query params format on the API, the query requested and
        // provided in the queryParams arg will be converted to a formatted string, e.g. "paramName:paramValue"
        // to allow reuse of existing implementation.
        final String propertyFilter = buildPropertyFilter(queryParams);

        final FilterQuery filterQuery = new FilterQuery(propertyFilter, sort, limit, offset);
        final Errors errors = this.getErrorsObj(filterQuery.getClass());
        ValidationUtils.invokeValidator(filterQueryValidator, filterQuery, errors);

        if (errors.hasErrors()) {
            final StringBuilder errorDetails = new StringBuilder();
            for (final FieldError fieldError : errors.getFieldErrors()) {
                if (!errorDetails.isEmpty()) {
                    errorDetails.append(COMMA).append(SPACE);
                }
                errorDetails
                    .append(fieldError.getField())
                    .append(SPACE).append(EQUAL).append(SPACE)
                    .append(fieldError.getDefaultMessage());
            }
            throw new OnboardingJobException(HttpStatus.BAD_REQUEST, errorDetails.toString());
        }
        return filterQuery;
    }

    private String buildPropertyFilter(final Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            // no property filter requested in query params
            return null;
        }

        // For now, only a single query parameter is allowed per request
        if (queryParams.size() == 1) {
            final Map.Entry<String, String> entry = queryParams.entrySet().iterator().next();
            final String queryParamName = entry.getKey();
            final String queryParamValue = entry.getValue();
            return String.format("%s:%s", queryParamName, queryParamValue);
        } else {
            throw new OnboardingJobException(HttpStatus.BAD_REQUEST, QUERY_MULTIPLE_PROPERTIES_NOT_ALLOWED);
        }
    }

    private OnboardingJob getJobById(final UUID jobId) {
        final Optional<OnboardingJobEntity> onboardingJobEntity = onboardingJobRepository.findById(jobId);
        try {
            if (onboardingJobEntity.isPresent()) {
                return onboardingJobMapper.toOnboardingJob(onboardingJobEntity.get());
            }
        } catch (final NoSuchElementException ex) {
            logger.error(String.format(ErrorMessages.ONBOARDING_JOB_ENTITY_NOT_FOUND, jobId), ex);
        }
        final String errorDetail = String.format(ErrorMessages.ONBOARDING_JOB_ENTITY_NOT_FOUND, jobId);
        throw new OnboardingJobException(HttpStatus.NOT_FOUND, errorDetail);
    }

    private OnboardingJobSearchSpecification parse(final String value) {
        String[] keyValue = value.split(Consts.QUERY_PARAM_KEY_VALUE_SEPARATOR, 2);
        if (keyValue[0].equalsIgnoreCase(FilterQuery.VALID_V2_FILTER_FIELDS.ID.toString())){
            return new OnboardingJobSearchSpecification(keyValue[0], UUID.fromString(keyValue[1]));
        } else if (keyValue[0].equalsIgnoreCase(FilterQuery.VALID_V2_FILTER_FIELDS.STATUS.toString())){
            return new OnboardingJobSearchSpecification(keyValue[0], OnboardingJobStatus.fromValue(keyValue[1]));
        } else {
            return new OnboardingJobSearchSpecification(keyValue[0], keyValue[1]);
        }
    }

    private void validateUploadedFileName(final MultipartFile file){
        final Errors errors = this.getErrorsObj(file.getClass());
        ValidationUtils.invokeValidator(fileNameValidator, file, errors);
        if (errors.hasErrors()) {
            logger.error("validateUploadedFileName() failed to create onboarding-job for App package. Reason: {}", errors);
            final String errorDetail = buildErrorDetail(errors);
            throw new OnboardingJobException(HttpStatus.BAD_REQUEST, errorDetail);
        }
    }

    private Errors getErrorsObj(final Class<?> clazz) {
        return new MapBindingResult(new HashMap<>(), clazz.getSimpleName());
    }

    private String buildErrorDetail(final Errors errors) {
        StringBuilder errorDetail = new StringBuilder();
        for (FieldError fieldError : errors.getFieldErrors()){
            if(!errorDetail.isEmpty()){
                errorDetail.append(Consts.STRING_SEPARATOR);
            }
            errorDetail.append(fieldError.getDefaultMessage());
        }
        return errorDetail.append(Consts.STRING_SEPARATOR).append(PLEASE_ONBOARD_VALID_CSAR).toString();
    }

    private OnboardingJobLink getOnboardingJobLink(final UUID onboardingJobId){
        return new OnboardingJobLink().id(onboardingJobId).href(urlGenerator.generateOnboardedJobLinkForExternalClient(String.valueOf(onboardingJobId)));
    }
}
