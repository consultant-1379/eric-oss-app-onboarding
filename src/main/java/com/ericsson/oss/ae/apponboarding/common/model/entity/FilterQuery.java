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

package com.ericsson.oss.ae.apponboarding.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class FilterQuery {
    public enum VALID_FILTER_FIELDS {NAME, VENDOR, VERSION, ID}

    @AllArgsConstructor
    @Getter
    public enum VALID_V2_FILTER_FIELDS {
        FILENAME("fileName"),
        VENDOR("vendor"),
        PACKAGEVERSION("packageVersion"),
        ID("id"),
        TYPE("type"),
        PACKAGESIZE("packageSize"),
        STATUS("status"),
        APPID("appId");

        private final String queryFieldName;
    }

    private final String queryParam;
    private final String sort;
    private final String limit;
    private final String offset;

    public FilterQuery(String findBy, String orderBy, String pageLimit, String pageIndex) {
        this.queryParam = findBy;
        this.sort = orderBy;
        this.limit = pageLimit;
        this.offset = pageIndex;
    }

    public String getQueryParam() {
        return queryParam;
    }

    public String getSort() {
        return sort;
    }

    public String getLimit() {
        return limit;
    }

    public String getOffset() {
        return offset;
    }
}
