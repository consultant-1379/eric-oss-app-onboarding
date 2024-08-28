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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.scanner.ScannerException;

import com.ericsson.oss.ae.apponboarding.v2.consts.ParserConstants;
import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;

public class ParserHelper {
    private static final Logger logger = LoggerFactory.getLogger(ParserHelper.class);

    public static Map<String, Object> parseYamlFile(final Path filePath) throws IOException {
        try {
            final LoaderOptions loadOptions = new LoaderOptions();
            loadOptions.setAllowDuplicateKeys(Boolean.FALSE);
            final DumperOptions dumpOptions = new DumperOptions();
            final Yaml yaml = new Yaml(new SafeConstructor(loadOptions), new Representer(dumpOptions), dumpOptions, loadOptions);
            return yaml.load(Files.readString(filePath));
        } catch (final ScannerException | ParserException | DuplicateKeyException e) {
            String errMessage = String.format(ErrorMessages.PARSE_YAML_ERROR, filePath.getFileName());
            logger.error("parseYamlFile() {}. Reason: {}", errMessage, e.getMessage());
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMessage);
        }
    }

    public static void checkIsEmpty(final Object value, final String... args) {
        checkIsEmptyWithMessage(value, ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, args);
    }

    public static void checkIsEmptyWithMessage(final Object value, final String message, String... args) {
        boolean isEmpty = checkIsEmptyOrNull(value);

        if (isEmpty) {
            final String errMsg = String.format(message, (Object[]) args);
            logger.error("checkIsEmptyWithMessage() {}", errMsg);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMsg);
        }
    }

    public static boolean checkIsEmptyOrNull(final Object value) {
        boolean isEmpty = (value == null);
        if (value instanceof Map<?, ?>) {
            isEmpty = ((Map<?, ?>) value).isEmpty();
        } else if (value instanceof String) {
            isEmpty = StringUtils.isBlank((String) value);
        }
        return isEmpty;
    }

    public static void checkAppComponentKeysInYaml(final Map<String, Object> appDescriptorYamlFile, final String... args) {
        if (appDescriptorYamlFile.containsKey(ParserConstants.DESCRIPTOR_APPCOMPONENT_LIST) &&
            appDescriptorYamlFile.containsKey(ParserConstants.DESCRIPTOR_APPCOMPONENT)) {
            String errMsg = String.format(ErrorMessages.PARSER_MANDATORY_FIELD_ERROR, (Object[]) args);
            logger.error("checkAppComponentKeysInYaml() {}", errMsg);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMsg);
        }
    }

    public static void checkIfFileExist(final Path path, final String... param) {
        if (Files.notExists(path)) {
            final String errMsg = String.format(ErrorMessages.PARSER_FILE_NOT_EXIST_ERROR, (Object[]) param);
            logger.error("checkIfFileExist() {}", errMsg);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMsg);
        }
    }

    private ParserHelper() {
    }
}
