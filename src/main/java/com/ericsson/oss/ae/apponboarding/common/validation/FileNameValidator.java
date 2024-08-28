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

package com.ericsson.oss.ae.apponboarding.common.validation;

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;

/**
 * The FileNameValidator class provides methods to validate file names.
 * It helps to ensure that file names comply with certain rules and are safe to use in file systems.
 */
@Component
public class FileNameValidator implements Validator{

    /**
     * Sets up the Spring Validator to support validation of MultipartFile instances.
     * @param clazz The object to be checked for support.
     * @return {@code true} if the validator supports the object, {@code false} otherwise.
     */
    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return MultipartFile.class.isAssignableFrom(clazz);
    }

    /**
     * Validates a MultipartFile object to ensure the safety of its file name.
     * This method checks if the file name is not null and ends with .csar,
     * and if it contains any unsafe characters or patterns.
     * It adds an error to the provided Errors object if it fails the safety check.
     *
     * @param target The MultipartFile object to be validated.
     * @param errors Errors object to which validation errors are added.
     */
    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        final MultipartFile file = (MultipartFile) target;
        final String fileName = file.getOriginalFilename();
        if (!isFileNameACsar(fileName)){
            errors.rejectValue(Consts.FILE_TYPE_ERR_ID, Consts.FILE_TYPE_ERR_CODE, String.format(Consts.FILE_TYPE_INVALID, fileName));
        }
        if (!isFileNameSafe(fileName)){
            errors.rejectValue(Consts.FILE_NAME_ERR_ID, Consts.FILE_NAME_ERR_CODE, String.format(Consts.FILE_NAME_INVALID, fileName));
        }
    }

    private boolean isFileNameSafe(final String fileName) {
        if (fileName == null) {
            return false;
        }
        return !fileName.contains(Consts.PARENT_DIR) && !fileName.contains(Consts.DIR_SEPARATOR_LINUX) && !fileName.contains(Consts.DIR_SEPARATOR_WINDOWS);
    }

    private boolean isFileNameACsar(final String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.getDefault()).endsWith(Consts.CSAR_EXTENSION);
    }
}
