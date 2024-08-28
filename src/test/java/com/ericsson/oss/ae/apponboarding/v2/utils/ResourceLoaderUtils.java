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

package com.ericsson.oss.ae.apponboarding.v2.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

/**
 * Utility class used for reading files from classpath.
 */
public final class ResourceLoaderUtils {

    private ResourceLoaderUtils() {
    }

    /**
     * Method used to get a resource as an {@link InputStream} from a given classpath.
     *
     * @param path
     *            the path to the resource you want to load
     * @return the loaded resource as an {@link InputStream}
     * @throws IllegalArgumentException
     *             thrown if the supplied path has no resource to load
     */
    public static InputStream getClasspathResourceAsStream(final String path) {
        final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

        if (inputStream == null) {
            throw new IllegalArgumentException(String.format("InputStream is null for path '%s'", path));
        }

        return inputStream;
    }

    /**
     * Method used to get a resource as a {@link String} from a given classpath.
     *
     * @param path
     *            the path to the resource you want to load
     * @return the loaded resource as a {@link String}, or an empty {@link String}
     * @throws IllegalArgumentException
     *             thrown if the supplied path has no resource to load
     * @throws IOException
     *             thrown if there is any error loading the file or converting it
     */
    public static String getClasspathResourceAsString(final String path) throws IOException {
        try (InputStream inputStream = getClasspathResourceAsStream(path)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }


    public static String getClasspathResourceAsString(final String path, final String placeholder, final String replacementValue) throws IOException {
        String content = getClasspathResourceAsString(path);
        return content.replace(placeholder, replacementValue);
    }
}