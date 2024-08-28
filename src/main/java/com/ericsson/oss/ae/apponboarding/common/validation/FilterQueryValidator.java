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

package com.ericsson.oss.ae.apponboarding.common.validation;

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.common.model.entity.FilterQuery;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import java.util.Arrays;
import java.util.Optional;

@Primary
@Component
public class FilterQueryValidator implements Validator {

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return FilterQuery.class.equals(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        FilterQuery filterQuery = (FilterQuery) target;
        validateQueryParam(filterQuery, errors);
        validateSortBy(filterQuery, errors);
        validateLimit(filterQuery, errors);
        validateOffset(filterQuery, errors);
    }

    public Pageable getPageable(final FilterQuery filter, final Sort sortCriteria) {

        Pageable pageable=null;

        if (!Optional.ofNullable(filter.getOffset()).orElse("").isEmpty()) {
            pageable = PageRequest.of(this.getIntValue(filter.getOffset()), this.getIntValue(filter.getLimit()), sortCriteria);
        } else if (!Optional.ofNullable(filter.getLimit()).orElse("").isEmpty()) {
            pageable = PageRequest.of(Consts.DEFAULT_OFFSET, this.getIntValue(filter.getLimit()), sortCriteria);
        }

        return pageable;
    }

    public Sort getSortCriteria(final FilterQuery filter, Sort sortCriteria) {
        if (!Optional.ofNullable(filter.getSort()).orElse("").isEmpty()) {
            sortCriteria = buildSortCriteria(filter.getSort());
        }
        return sortCriteria;
    }

    public Sort buildSortCriteria(final String sortBy) {
        return Sort.by(Sort.Order.asc(sortBy));
    }

    private int getIntValue(final String val) {
        return Integer.parseInt(val);
    }

    protected void validateQueryParam(final FilterQuery filterQuery, final Errors errors) {
        if (null != filterQuery.getQueryParam() && !filterQuery.getQueryParam().isEmpty()) {

            String[] keyValue = filterQuery.getQueryParam().split(Consts.QUERY_PARAM_KEY_VALUE_SEPARATOR);

            if (keyValue.length < 2 && !filterQuery.getQueryParam().contains(Consts.QUERY_PARAM_KEY_VALUE_SEPARATOR)) {
                errors.rejectValue(Consts.QUERY_PARAM_ERR_ID, Consts.QUERY_PARAM_INVALID_FORMAT_ERR_CODE, Consts.QUERY_PARAM_INVALID_FORMAT_ERR_MSG);
            } else if (keyValue.length < 2 && filterQuery.getQueryParam().contains(Consts.QUERY_PARAM_KEY_VALUE_SEPARATOR)) {
                errors.rejectValue(Consts.QUERY_PARAM_ERR_ID, Consts.QUERY_PARAM_EMPTY_VALUE_ERR_CODE, Consts.QUERY_PARAM_EMPTY_VALUE_ERR_MSG);
            } else if (!this.isFieldInValidList(keyValue[0])) {
                errors.rejectValue(Consts.QUERY_PARAM_ERR_ID, Consts.QUERY_PARAM_UNKNOWN_FIELD_ERR_CODE, Consts.QUERY_PARAM_UNKNOWN_FIELD_ERR_MSG);
            } else if (keyValue[0].equalsIgnoreCase(FilterQuery.VALID_FILTER_FIELDS.ID.toString()) && !this.isPositiveNumber(keyValue[1])) {
                errors.rejectValue(Consts.QUERY_PARAM_ERR_ID, Consts.QUERY_PARAM_INVALID_ID_FORMAT_ERR_CODE,
                    Consts.QUERY_PARAM_INVALID_ID_FORMAT_ERR_MSG);
            }
        }
    }

    protected void validateSortBy(final FilterQuery filterQuery, final Errors errors) {
        if (null != filterQuery.getSort() && !filterQuery.getSort().isEmpty() && !this.isFieldInValidList(filterQuery.getSort())) {
            errors.rejectValue(Consts.SORT_ERR_ID, Consts.SORT_ERR_CODE, Consts.SORT_ERR_MSG);
        }
    }

    protected void validateLimit(final FilterQuery filterQuery, final Errors errors) {
        if (null != filterQuery.getLimit() && !filterQuery.getLimit().isEmpty() && !this.isPositiveNumber(filterQuery.getLimit())) {
            errors.rejectValue(Consts.PAGE_LIMIT_ERR_ID, Consts.PAGE_LIMIT_ERR_CODE, Consts.PAGE_LIMIT_ERR_MSG);
        }
    }

    protected void validateOffset(final FilterQuery filterQuery, final Errors errors) {
        if (null != filterQuery.getOffset() && !filterQuery.getOffset().isEmpty() && !this.isPositiveNumber(filterQuery.getOffset())) {
            errors.rejectValue(Consts.OFFSET_ERR_ID, Consts.OFFSET_ERR_CODE, Consts.OFFSET_ERR_MSG);
        }
        if (null != filterQuery.getOffset() && !filterQuery.getOffset().isEmpty() && (null == filterQuery.getLimit() || filterQuery.getLimit()
            .isEmpty())) {
            errors.rejectValue(Consts.OFFSET_LIMIT_ERR_ID, Consts.OFFSET_LIMIT_ERR_CODE, Consts.OFFSET_LIMIT_ERR_MSG);
        }
    }

    protected boolean isFieldInValidList(final String value) {
        return Arrays.stream(FilterQuery.VALID_FILTER_FIELDS.class.getEnumConstants()).anyMatch(e -> e.name().equalsIgnoreCase(value));
    }

    protected boolean isPositiveNumber(final String value) {
        try {
            int intValue = Integer.parseInt(value);
            return intValue >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
