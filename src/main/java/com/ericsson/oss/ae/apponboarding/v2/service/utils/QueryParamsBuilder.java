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
package com.ericsson.oss.ae.apponboarding.v2.service.utils;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Builds the user provided property filter parameter. Only non-null values are stored in the QueryParam map.
 *
 */
@Getter
public class QueryParamsBuilder {

    private final Map<String, String> queryParam = new HashMap<>();

    public QueryParamsBuilder id(final String id) {
        if (id != null) {
            queryParam.put("id", id);
        }
        return this;
    }

    public QueryParamsBuilder fileName(final String fileName) {
        if (fileName != null) {
            queryParam.put("fileName", fileName);
        }
        return this;
    }

    public QueryParamsBuilder vendor(final String vendor) {
        if (vendor != null) {
            queryParam.put("vendor", vendor);
        }
        return this;
    }

    public QueryParamsBuilder type(final String type) {
        if (type != null) {
            queryParam.put("type", type);
        }
        return this;
    }

    public QueryParamsBuilder packageVersion(final String packageVersion) {
        if (packageVersion != null) {
            queryParam.put("packageVersion", packageVersion);
        }
        return this;
    }

    public QueryParamsBuilder packageSize(final String packageSize) {
        if (packageSize != null) {
            queryParam.put("packageSize", packageSize);
        }
        return this;
    }

    public QueryParamsBuilder status(final String status) {
        if (status != null) {
            queryParam.put("status", status);
        }
        return this;
    }

    public QueryParamsBuilder appId(final String appId) {
        if (appId != null) {
            queryParam.put("appId", appId);
        }
        return this;
    }

}
