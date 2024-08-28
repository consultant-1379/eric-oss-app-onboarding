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

package com.ericsson.oss.ae.apponboarding.common.controller.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.TEMP_FILE_ATT_NAME;

@Component
public class TempFileCleanupInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TempFileCleanupInterceptor.class);

    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception ex)
        throws IOException {
        final Object tmpFile = request.getAttribute(TEMP_FILE_ATT_NAME);
        if (ObjectUtils.isEmpty(tmpFile)) {
            logger.warn("Cannot delete temp file");
        } else {
            logger.info("Deleting temp file {} ", tmpFile);
            Files.delete(Paths.get(tmpFile.toString()));
            logger.info("Deleted temp file {} ", tmpFile);
        }
    }
}
