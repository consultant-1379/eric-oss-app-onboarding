/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

import java.util.HashMap;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.common.model.entity.FilterQuery;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { FilterQueryValidatorV2.class })
public class FilterQueryValidatorV2UnitTest {
    @Autowired private FilterQueryValidatorV2 filterQueryValidator;

    private Errors errors;

    @BeforeEach
    public void setUp() {
        errors = new MapBindingResult(new HashMap<String, String>(), FilterQuery.class.getSimpleName());
    }

    @Test
    public void testInvalidSortAndPageParams() {
        FilterQuery filterQuery = new FilterQuery("fileName:test.csar", "sort", "abc", "a");
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertFalse(this.errors.getFieldErrors().isEmpty());
        Assert.assertEquals(3, this.errors.getFieldErrors().size());
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.SORT_V2_ERR_MSG));
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.PAGE_LIMIT_ERR_MSG));
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.OFFSET_ERR_MSG));
    }

    @Test
    public void testAllValidQueryFilterParam() {
        FilterQuery queryFilter = new FilterQuery("fileName:abcd", "fileName", "10", "1");
        filterQueryValidator.validate(queryFilter, errors);
        Assert.assertTrue(this.errors.getFieldErrors().isEmpty());
    }

    @Test
    public void testFindByInvalidValue() {
        FilterQuery filterQuery = new FilterQuery("vendor:", null, null, null);
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertFalse(this.errors.getFieldErrors().isEmpty());
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.QUERY_PARAM_EMPTY_VALUE_ERR_MSG));
    }

    @Test
    public void testFindByIDInvalidNumber() {
        FilterQuery filterQuery = new FilterQuery("id:abc", null, null, null);
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertFalse(this.errors.getFieldErrors().isEmpty());
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.QUERY_PARAM_INVALID_ID_FORMAT_V2_ERR_MSG));
    }

    @Test
    public void testOrderByInvalidKeyField() {
        FilterQuery filterQuery = new FilterQuery(null, "abc", null, null);
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertFalse(this.errors.getFieldErrors().isEmpty());
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.SORT_V2_ERR_MSG));
    }

    @Test
    public void testPageLimitInvalidNumber() {
        FilterQuery filterQuery = new FilterQuery(null, null, "-1", null);
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertFalse(this.errors.getFieldErrors().isEmpty());
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.PAGE_LIMIT_ERR_MSG));
    }

    @Test
    public void testPageIndexInvalidNumber() {
        FilterQuery filterQuery = new FilterQuery(null, null, "1", "-1");
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertFalse(this.errors.getFieldErrors().isEmpty());
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.OFFSET_ERR_MSG));
    }

    @Test
    public void testPageIndexWhenPageLimitNotGiven() {
        FilterQuery filterQuery = new FilterQuery(null, null, null, "1");
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertFalse(this.errors.getFieldErrors().isEmpty());
        Assert.assertThat(errors.toString(), CoreMatchers.containsString(Consts.OFFSET_LIMIT_ERR_MSG));
    }

    @Test
    public void testValidIdValidationOfFindBy(){
        FilterQuery filterQuery = new FilterQuery("id:"+UUID.randomUUID(),null,null,null);
        filterQueryValidator.validate(filterQuery, errors);
        Assert.assertTrue(this.errors.getFieldErrors().isEmpty());
    }
}
