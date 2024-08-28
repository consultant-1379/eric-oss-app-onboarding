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

package com.ericsson.oss.ae.apponboarding.v2.service.model.dto.lcm;

import java.util.List;
import java.util.UUID;

import com.ericsson.oss.ae.apponboarding.v2.service.model.dto.Component;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Contains only the App properties that are used in App Onboarding.   When mapping responses from LCM containing
 * the full App resource properties, only the properties defined here are captured, and all the others are
 * ignored.
 * If other LCM App properties are to be used then add them here.
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppDetails {

    private UUID id;

    private String name;

    private String version;

    private String provider;

    private List<Component> components = null;
}