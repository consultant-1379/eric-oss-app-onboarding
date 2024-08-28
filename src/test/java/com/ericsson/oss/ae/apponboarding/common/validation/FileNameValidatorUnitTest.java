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

package com.ericsson.oss.ae.apponboarding.common.validation;

import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;

import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FILE_NAME_UNSAFE_CSAR;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FILE_NAME_VALID_CSAR;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FILE_TYPE_INVALID_CSAR;
import static com.ericsson.oss.ae.apponboarding.common.consts.Consts.FILE_TYPE_AND_NAME_INVALID_CSAR;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { FileNameValidator.class })
public class FileNameValidatorUnitTest {

    @Autowired FileNameValidator fileNameValidator;

    private Errors errors;

    @BeforeEach
    public void setUp(){
        errors = new MapBindingResult(new HashMap<String, MultipartFile>(), "fileName");
    }

    @Test
    public void testSupportMultipartFileClass() {
        Assert.assertTrue(fileNameValidator.supports(MultipartFile.class));
    }

    @Test
    public void testSupportNotMultipartFileClass() {
        Assert.assertFalse(fileNameValidator.supports(String.class));
    }

    @Test
    public void testFileNameNotSafe() {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_NAME_UNSAFE_CSAR, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        fileNameValidator.validate(file, errors);
        Assert.assertEquals(1, this.errors.getFieldErrors().size());
        MatcherAssert.assertThat(errors.toString(), CoreMatchers.containsString(String.format(Consts.FILE_NAME_INVALID, file.getOriginalFilename())));
    }

    @Test
    public void testFileTypeNotACSAR() {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_TYPE_INVALID_CSAR, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        fileNameValidator.validate(file, errors);
        Assert.assertEquals(1, this.errors.getFieldErrors().size());
        MatcherAssert.assertThat(errors.toString(), CoreMatchers.containsString(String.format(Consts.FILE_TYPE_INVALID, file.getOriginalFilename())));
    }

    @Test
    public void testFileTypeNotACSAR_AndFileNameNotSafe() {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_TYPE_AND_NAME_INVALID_CSAR, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        fileNameValidator.validate(file, errors);
        Assert.assertEquals(2, this.errors.getFieldErrors().size());
        MatcherAssert.assertThat(errors.toString(), CoreMatchers.containsString(String.format(Consts.FILE_TYPE_INVALID, file.getOriginalFilename())));
        MatcherAssert.assertThat(errors.toString(), CoreMatchers.containsString(String.format(Consts.FILE_NAME_INVALID, file.getOriginalFilename())));
    }

    @Test
    public void testFileNameSafe() {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_NAME_VALID_CSAR, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        fileNameValidator.validate(file, errors);
        Assert.assertEquals(0, this.errors.getFieldErrors().size());
    }

}
