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

package com.ericsson.oss.ae.apponboarding.common.consts;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditLogMessages {
    //V1 Keywords
    public static final String V1_ONBOARD_AN_APP = "ONBOARD App {%s} using CSAR {%s}";
    public static final String V1_ENABLE_APP = "ENABLE App {%s}";
    public static final String V1_DISABLE_APP = "DISABLE App {%s}";
    public static final String V1_DELETING_APP = "DELETING App {%s}";
    public static final String V1_DELETE_APP_BY_ID = "DELETE App {%s}";

    //V2 Keywords
    public static final String V2_DELETE_ONBOARDING_JOB_BY_ID = "DELETE Onboarding Job {%s}";
    public static final String V2_ONBOARD_APP_PACKAGE = "ONBOARD App Package {%s} using CSAR {%s}";

}
