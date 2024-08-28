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

package com.ericsson.oss.ae.apponboarding.common.consts.utils;

public class StringUtils {

    private StringUtils() {
    }

    public static String append(final boolean withSpace, final Object... objects) {
        final StringBuilder sb = new StringBuilder();
        for (final Object o : objects) {
            sb.append(o.toString());
            if (withSpace) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static String append(final Object... objects) {
        return append(false, objects);
    }

    public static String appendWithSpace(final Object... objects) {
        return append(true, objects);
    }

}
