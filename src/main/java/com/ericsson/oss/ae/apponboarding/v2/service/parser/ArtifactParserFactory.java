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

package com.ericsson.oss.ae.apponboarding.v2.service.parser;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ericsson.oss.ae.apponboarding.v2.exception.ErrorMessages;
import com.ericsson.oss.ae.apponboarding.v2.exception.OnboardingJobException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ArtifactParserFactory {
    @Autowired private List<ArtifactParser> artifactParsers;

    private static final Logger logger = LoggerFactory.getLogger(ArtifactParserFactory.class);

    public ArtifactParser getParser(final String type) {
        final Optional<ArtifactParser> parser = artifactParsers.stream().filter(p -> p.isSupported(type)).findAny();

        if (parser.isPresent()) {
            return parser.get();
        } else {
            String errMessage = String.format(ErrorMessages.NONE_SUPPORTED_ARTIFACT_PARSER_ERROR, type);
            logger.error("getParser() {}", errMessage);
            throw new OnboardingJobException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.PARSER_PROBLEM_TITLE, errMessage);
        }
    }
}
