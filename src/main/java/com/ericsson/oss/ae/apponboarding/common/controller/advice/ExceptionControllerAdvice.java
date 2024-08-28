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

package com.ericsson.oss.ae.apponboarding.common.controller.advice;


import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.DATA_ACCESS_EXCEPTION_DETAIL;
import static com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages.TRANSACTION_EXCEPTION_DETAIL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.ericsson.oss.ae.apponboarding.api.v2.model.ProblemDetails;

import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingException;
import com.ericsson.oss.ae.apponboarding.v1.model.exception.AppOnboardingValidationException;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler(OnboardingJobException.class)
    public ResponseEntity<ProblemDetails> handleAppOnboardingException(final HttpServletRequest request, final OnboardingJobException ex) {
        logger.error("handleAppOnboardingException() {}: {} ", ex.getProblemDetails().getTitle(), ex.getProblemDetails().getDetail());
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(ex.getProblemDetails(), headers, ex.getResponseStatus());
    }

    @ExceptionHandler(AppOnboardingException.class)
    public ResponseEntity handleAppOnboardingException(final HttpServletRequest request, final AppOnboardingException ex) {
        final Map<String, Object> atts = new HashMap();
        atts.put("timestamp", new Date());
        atts.put("error", ex.getMessage());
        atts.put("errorCode", ex.getErrorCode());
        atts.put("path", request.getRequestURI());
        atts.put("method", request.getMethod());
        atts.put("status", ex.getHttpStatus());
        logger.error("AppOnboardingException occurred", ex);
        return new ResponseEntity<>(atts, ex.getHttpStatus());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity handleEntityNotFoundException(EntityNotFoundException e, final HttpServletRequest request, HttpServletResponse response) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity handleTransactionException(TransactionException e, final HttpServletRequest request, HttpServletResponse response) {
        logger.error("handleTransactionException() Transaction Exception caught: Details : {}", e.getMessage());

        if (request.getRequestURI().contains("v1")) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            final String detail = String.format(TRANSACTION_EXCEPTION_DETAIL, e.getMessage());
            final ProblemDetails problemDetails = createProblemDetails( detail, HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(problemDetails, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity handleDataAccessException(DataAccessException e, final HttpServletRequest request, HttpServletResponse response) {
        logger.error("handleTransactionException() Transaction Exception caught: Details : {}", e.getMessage());

        if (request.getRequestURI().contains("v1")) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            final String detail = String.format(DATA_ACCESS_EXCEPTION_DETAIL, e.getMessage());
            final ProblemDetails problemDetails = createProblemDetails(detail, HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(problemDetails, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @ExceptionHandler(AppOnboardingValidationException.class)
    public ResponseEntity handleAppOnboardingValidationException(final HttpServletRequest request, final AppOnboardingValidationException ex) {
        final Map<String, Object> atts = new HashMap<>();
        atts.put("timestamp", new Date());

        if (ex.getErrors() != null) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError fieldError : ex.getErrors().getFieldErrors()) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
            atts.put("error", errors);
            logger.info("AppOnboardingValidationException occurred: {}", errors);
        }

        atts.put("errorCode", ex.getErrorCode());
        atts.put("path", request.getRequestURI());
        atts.put("method", request.getMethod());
        atts.put("status", ex.getHttpStatus());
        return new ResponseEntity<>(atts, ex.getHttpStatus());
    }

    private ProblemDetails createProblemDetails(final String detail, HttpStatus httpStatus) {
        return new ProblemDetails()
            .title(httpStatus.getReasonPhrase())
            .status(httpStatus.value())
            .detail(detail);
    }
}
