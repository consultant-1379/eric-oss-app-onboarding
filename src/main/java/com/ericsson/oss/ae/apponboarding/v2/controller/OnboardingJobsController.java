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

package com.ericsson.oss.ae.apponboarding.v2.controller;

import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V2_ONBOARD_APP_PACKAGE;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogMessages.V2_DELETE_ONBOARDING_JOB_BY_ID;
import static com.ericsson.oss.ae.apponboarding.common.consts.AuditLogConstants.ADDITIONAL_MESSAGE_KEY;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.APP_ONBOARDING_V2;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.SUBJECT_NOT_AVAILABLE;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.ae.apponboarding.api.v2.AppPackagesApi;
import com.ericsson.oss.ae.apponboarding.api.v2.OnboardingJobsApi;
import com.ericsson.oss.ae.apponboarding.api.v2.model.AppPackageResponse;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJob;
import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobItems;
import com.ericsson.oss.ae.apponboarding.v2.service.OnboardingJobsService;
import com.ericsson.oss.ae.apponboarding.v2.service.utils.QueryParamsBuilder;

import jakarta.ws.rs.QueryParam;

@RestController
@RequestMapping(value = APP_ONBOARDING_V2, produces = MediaType.APPLICATION_JSON_VALUE)
public class OnboardingJobsController implements OnboardingJobsApi, AppPackagesApi {

    @Autowired
    private OnboardingJobsService onboardingJobsService;

    @Autowired
    private NativeWebRequest nativeWebRequest;

    private Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(nativeWebRequest);
    }

    private static final Logger logger = LoggerFactory.getLogger(OnboardingJobsController.class);

    /**
     * Invokes a POST request to onboard an app-package. To onboard the package a new onboarding-job
     * is created in the system to handle the request.
     *
     * @param file
     *            App-package file to be uploaded in .csar format (required).
     * @return Returns a ResponseEntity containing the HTTP Status.
     */
    @Override
    public ResponseEntity<AppPackageResponse> onboardAppPackage(@RequestPart final MultipartFile file) {
        final String fileName = file.getOriginalFilename();
        logger.info("onboardAppPackage() request made to onboard csar: {} with the size of {}", fileName, file.getSize());
        setAdditionalAuditLogMessage(String.format(V2_ONBOARD_APP_PACKAGE, SUBJECT_NOT_AVAILABLE, fileName));
        final AppPackageResponse appPackageResponse = onboardingJobsService.onboardAppPackage(file);
        logger.info("onboardAppPackage() accepted the service request and created a new onboarding-job with id {}", appPackageResponse.getOnboardingJob().getId());
        setAdditionalAuditLogMessage(String.format(V2_ONBOARD_APP_PACKAGE, appPackageResponse.getOnboardingJob().getId(), fileName));
        return new ResponseEntity<>(appPackageResponse, HttpStatus.ACCEPTED);
    }

    /**
     * Invokes a GET request to Return a list of Onboarding Jobs.
     *
     * @param id Filter parameter id - Optional
     *
     * @param sort
     *            Get Apps Sorted By (name, vendor, version, id) (optional)
     * @param offset
     *            Get Apps Filter by Offset (optional, default to &quot;0&quot;)
     * @param limit
     *            Get Apps Filter by limit (optional)
     * @return
     */
    @Override
    public ResponseEntity<OnboardingJobItems>  getAllOnboardingJobs(@RequestHeader("Accept") final String accept,
                                                                    @QueryParam(value = "id") final String id,
                                                                    @QueryParam(value = "fileName") final String fileName,
                                                                    @QueryParam(value = "vendor") final String vendor,
                                                                    @QueryParam(value = "packageVersion") final String packageVersion,
                                                                    @QueryParam(value = "type") final String type,
                                                                    @QueryParam(value = "packageSize") final String packageSize,
                                                                    @QueryParam(value = "status") final String status,
                                                                    @QueryParam(value = "appId") final String appId,
                                                                    @QueryParam(value = "sort") final String sort,
                                                                    @QueryParam(value = "offset") final String offset,
                                                                    @QueryParam(value = "limit") final String limit) {
        logger.info("getAllOnboardingJob() request made to onboard jobs list, with filtering options " +
                "id={}, fileName={}, vendor={}, packageVersion={}, type={}, packageSize={}, status={}, appId={}, " +
                "sort={}, offset={}, limit={}", id, fileName, vendor, packageVersion, type, packageSize, status, appId, sort, offset, limit);


        // Put non-null filter params in a Map
        final QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .id(id)
            .fileName(fileName)
            .vendor(vendor)
            .packageVersion(packageVersion)
            .packageSize(packageSize)
            .type(type)
            .status(status)
            .appId(appId);
        final Map<String, String> filterParams = queryParamsBuilder.getQueryParam();
        final OnboardingJobItems items = onboardingJobsService.getOnboardingAllJobsList(filterParams, sort, offset, limit);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<OnboardingJob> getJobById(final @PathVariable UUID jobId,
                                                    final @RequestHeader(value = "Accept", required = false) String accept) {
        logger.info("Received a Get request to get an Onboarding job with id = {}", jobId);
        return new ResponseEntity<>(onboardingJobsService.getOnboardingJobById(jobId), HttpStatus.OK);
    }

    /**
     * Invokes a DELETE request to delete an Onboarding Job by Long jobId.
     *
     * @param jobId
     *            The id of the onboarding job to be deleted (required).
     * @return Returns a ResponseEntity containing the HTTP Status.
     */
    @Override
    public ResponseEntity<Void> deleteOnboardingJob(final UUID jobId, @RequestHeader(value = "Accept", required = false) final String accept) {
        logger.info("deleteOnboardingJob() request made to delete onboarding job with id: {}", jobId);
        setAdditionalAuditLogMessage(String.format(V2_DELETE_ONBOARDING_JOB_BY_ID, jobId));

        onboardingJobsService.deleteOnboardingJob(jobId);

        logger.info("deleteOnboardingJob() request completed successfully to delete onboarding job with id: {}", jobId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    private void setAdditionalAuditLogMessage(final String additionalMessage) {
        getRequest().ifPresent(request -> request.setAttribute(ADDITIONAL_MESSAGE_KEY, additionalMessage, NativeWebRequest.SCOPE_REQUEST));
    }
}
