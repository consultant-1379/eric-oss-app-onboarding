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

package com.ericsson.oss.ae.apponboarding.v2.validation;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.common.model.entity.FilterQuery;
import com.ericsson.oss.ae.apponboarding.common.validation.FilterQueryValidator;

@Component
public class FilterQueryValidatorV2 extends FilterQueryValidator {

    private static final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        FilterQuery filterQuery = (FilterQuery) target;
        this.validateQueryParam(filterQuery, errors);
        this.validateSortBy(filterQuery, errors);
        super.validateLimit(filterQuery, errors);
        super.validateOffset(filterQuery, errors);
    }

    @Override
    protected void validateQueryParam(FilterQuery filterQuery, Errors errors) {
        if (null != filterQuery.getQueryParam() && !filterQuery.getQueryParam().isEmpty()) {

            String[] keyValue = filterQuery.getQueryParam().split(Consts.QUERY_PARAM_KEY_VALUE_SEPARATOR);

            if (keyValue.length < 2 && filterQuery.getQueryParam().contains(Consts.QUERY_PARAM_KEY_VALUE_SEPARATOR)) {
                errors.rejectValue(Consts.QUERY_PARAM_ERR_ID, Consts.QUERY_PARAM_EMPTY_VALUE_ERR_CODE, Consts.QUERY_PARAM_EMPTY_VALUE_ERR_MSG);
            } else if (keyValue[0].equalsIgnoreCase(FilterQuery.VALID_V2_FILTER_FIELDS.ID.toString()) && !this.isValidUUID(keyValue[1])) {
                errors.rejectValue(Consts.QUERY_PARAM_ERR_ID, Consts.QUERY_PARAM_INVALID_ID_FORMAT_ERR_CODE,
                    Consts.QUERY_PARAM_INVALID_ID_FORMAT_V2_ERR_MSG);
            } else if (keyValue[0].equalsIgnoreCase(FilterQuery.VALID_V2_FILTER_FIELDS.STATUS.toString()) && !this.isValidStatus(keyValue[1])) {
                errors.rejectValue(Consts.QUERY_PARAM_ERR_ID, Consts.QUERY_PARAM_INVALID_STATUS_ERR_CODE, Consts.QUERY_PARAM_INVALID_STATUS_ERR_MSG);
            }
        }
    }

    private boolean isValidUUID(final String id){
        return UUID_PATTERN.matcher(id).matches();
    }

    private boolean isValidStatus(final String status) {
        for (OnboardingJobStatus jobStatus : OnboardingJobStatus.values()) {
            if (jobStatus.getValue().equals(status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void validateSortBy(final FilterQuery filterQuery, final Errors errors) {
        if (null != filterQuery.getSort() && !filterQuery.getSort().isEmpty() && !this.isFieldInValidList(filterQuery.getSort())) {
            errors.rejectValue(Consts.SORT_ERR_ID, Consts.SORT_ERR_CODE, Consts.SORT_V2_ERR_MSG);
        }
    }

    @Override
    protected boolean isFieldInValidList(final String value) {
        return Arrays.stream(FilterQuery.VALID_V2_FILTER_FIELDS.class.getEnumConstants()).anyMatch(e -> e.getQueryFieldName().equals(value));
    }
}
