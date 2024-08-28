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

package com.ericsson.oss.ae.apponboarding.v1.controller.onboarding;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.ericsson.oss.ae.apponboarding.v1.service.jobs.EnvironmentVariableCreator;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v1.TestKubernetesServerConfig;
import com.ericsson.oss.ae.apponboarding.v1.controller.ApplicationController;
import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, ApplicationController.class }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@ContextConfiguration(classes = TestKubernetesServerConfig.class)
public class ApplicationControllerTest {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${HELM_REG_URL}") private String helmUrl;

    private static String FILE_NAME = "hello.csar";
    private static final String ENV_VAR_NAME = "TEST_ENV_NAME";
    private static final String ENV_VAR_VALUE = "TEST_ENV_VALUE";

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mvc;
    private static final String APPS_VERSION = Consts.API_V1_APPS;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ArtifactRepository artifactRepository;

    @MockBean
    EnvironmentVariableCreator environmentVariableCreator;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void getAllApps() throws Exception {
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(get(APPS_VERSION).contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
    }

    @Test
    public void getAllAppsSortByID() throws Exception {
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(get(APPS_VERSION + "?sort=id").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
        ObjectMapper mapper = new ObjectMapper();

        List<Application> resultList = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        List<Application> orderedList = List.copyOf(resultList);

        List<Application> sortedListID = sortBy(resultList, "id");
        Assert.assertEquals(orderedList, sortedListID);

    }

    @Test
    public void getAllAppsSortByName() throws Exception {
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(get(APPS_VERSION + "?sort=name").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        List<Application> orderedList = List.copyOf(resultList);

        List<Application> sortedListName = sortBy(resultList, "name");
        Assert.assertEquals(orderedList, sortedListName);
    }

    @Test
    public void getAllAppsSortByVendor() throws Exception {
        MvcResult result = mvc.perform(get(APPS_VERSION + "?sort=vendor").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        List<Application> orderedList = List.copyOf(resultList);

        List<Application> sortedListVendor = sortBy(resultList, "vendor");
        Assert.assertEquals(orderedList, sortedListVendor);
    }

    @Test
    public void getAllAppsSortByVersion() throws Exception {
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(get(APPS_VERSION + "?sort=version").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        List<Application> orderedList = List.copyOf(resultList);

        List<Application> sortedListVersion = sortBy(resultList, "version");
        Assert.assertEquals(orderedList, sortedListVersion);
    }

    @Test
    public void getAllAppsSortByNameAndLimitToThree() throws Exception {
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(get(APPS_VERSION + "?sort=name&limit=3").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        Assert.assertEquals(3, resultList.size());
    }

    @Test
    public void getAppsFilterQueryByName() throws Exception {
        this.createTestApplication();

        MvcResult result = mvc.perform(get(APPS_VERSION + "?q=name:eric-oss-app-mgr").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        Assert.assertTrue(resultList.size() > 0);

    }

    @Test
    public void getAppsFilterQueryByVendor() throws Exception {
        Application application = this.createTestApplication();

        MvcResult result1 = mvc.perform(get(APPS_VERSION + "?q=vendor:Ericsson").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result1.getResponse().getStatus());
        Assert.assertThat(result1.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(application.getMode().toString())));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList1 = mapper.readValue(result1.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        Assert.assertTrue(resultList1.size() > 0);

    }

    @Test
    public void getAppsFilterQueryByNameAndSortByID() throws Exception {
        Application application = this.createTestApplication();

        MvcResult result = mvc.perform(get(APPS_VERSION + "?q=name:eric-oss-app-mgr&sort=id").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(application.getMode().toString())));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList2 = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        Assert.assertTrue(resultList2.size() > 0);

    }

    @Test
    public void getAppsFilterQueryByNameAndSortByIDAndOffsetOneAndLimitTwo() throws Exception {
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(
            get(APPS_VERSION + "?q=name:eric-oss-app-mgr&sort=id&offset=1&limit=2").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));

        ObjectMapper mapper = new ObjectMapper();
        List<Application> resultList2 = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Application>>() {
        });
        Assert.assertEquals(2, resultList2.size());

    }

    @Test
    public void getAppsFilterByRandomString() throws Exception {
        this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        MvcResult result = mvc.perform(get(APPS_VERSION + "?this is a test").contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
    }

    @Test
    public void getAppsFilterInvalidSearchColumnInQueryParam() throws Exception {
        mvc.perform(get(APPS_VERSION + "?q=name2:eric-oss-app-mgr").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
            .andExpect(result -> Assert.assertThat(result.getResponse().getContentAsString(),
                CoreMatchers.containsString(Consts.QUERY_PARAM_UNKNOWN_FIELD_ERR_MSG)));
    }

    @Test
    public void getAppsFilterInvalidSeparatorInQueryParam() throws Exception {
        mvc.perform(get(APPS_VERSION + "?q=name=eric-oss-app-mgr").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
            .andExpect(result -> Assert.assertThat(result.getResponse().getContentAsString(),
                CoreMatchers.containsString(Consts.QUERY_PARAM_INVALID_FORMAT_ERR_MSG)));
    }

    @Test
    public void getAppsFilterEmptyValueInQueryParam() throws Exception {
        mvc.perform(get(APPS_VERSION + "?q=name:").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(
            result -> Assert.assertThat(result.getResponse().getContentAsString(),
                CoreMatchers.containsString(Consts.QUERY_PARAM_EMPTY_VALUE_ERR_MSG)));
    }

    @Test
    public void getAppsFilterByTwoSeparatorInQueryParam() throws Exception {
        mvc.perform(get(APPS_VERSION + "?q=vendor:Ericsson:abc").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
            result -> Assert.assertThat(result.getResponse().getContentAsString(),
                CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED))));
    }

    @Test
    public void getAppsFilterByInvalidStringInQueryParam() throws Exception {
        mvc.perform(get(APPS_VERSION + "?q=this is a  test").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(
            result -> Assert.assertThat(result.getResponse().getContentAsString(),
                CoreMatchers.containsString(Consts.QUERY_PARAM_INVALID_FORMAT_ERR_MSG)));
    }

    @Test
    public void getAppsFilterByInvalidIDInQueryParam() throws Exception {
        mvc.perform(get(APPS_VERSION + "?q=id:abc").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(
            result -> Assert.assertThat(result.getResponse().getContentAsString(),
                CoreMatchers.containsString(Consts.QUERY_PARAM_INVALID_ID_FORMAT_ERR_MSG)));

    }

    @Test
    public void getAppsFilterByInvalidColumnInSort() throws Exception {
        mvc.perform(get(APPS_VERSION + "?sort=size").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
            .andExpect(result -> Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(Consts.SORT_ERR_MSG)));
    }

    @Test
    public void getAppsFilterByInvalidOffset() throws Exception {
        mvc.perform(get(APPS_VERSION + "?sort=name&offset=-1").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
            .andExpect(result -> Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(Consts.OFFSET_ERR_MSG)));
    }

    @Test
    public void getAppsFilterByOffsetOnly() throws Exception {
        MvcResult result = mvc.perform(get(APPS_VERSION + "?offset=1").contentType(MediaType.APPLICATION_JSON)).andReturn();

        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(Consts.OFFSET_LIMIT_ERR_MSG));
    }

    @Test
    public void getAppsFilterByNonExistingField() throws Exception {
        MvcResult result = mvc.perform(get(APPS_VERSION + "?q=abc:eric-oss-app-mgr").contentType(MediaType.APPLICATION_JSON)).andReturn();

        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(Consts.QUERY_PARAM_UNKNOWN_FIELD_ERR_MSG));
    }

    @Test
    public void getAppsSortByNonExistingField() throws Exception {
        MvcResult result = mvc.perform(get(APPS_VERSION + "?sort=abc").contentType(MediaType.APPLICATION_JSON)).andReturn();

        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(Consts.SORT_ERR_MSG));
    }

    @Test
    public void getAppById() throws Exception {
        Application application = this.createTestApplication();

        MvcResult result = mvc.perform(get(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
    }

    @Test
    public void getAppsInvalidId() throws Exception {
        mvc.perform(get(APPS_VERSION + "/not_int").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    public void getAppByNotExistingId() throws Exception {
        mvc.perform(get(APPS_VERSION + "/-1").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getAppArtifactsByArtifactId() throws Exception {
        Application application = this.createTestApplication();
        Artifact artifact = application.getArtifacts().iterator().next();

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + "/artifacts/" + artifact.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getAppArtifactsByNotExistingArtifactId() throws Exception {
        mvc.perform(get(APPS_VERSION + "/-1/artifacts/12").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getAppArtifactsById() throws Exception {
        Application application = this.createTestApplication();

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + "/artifacts").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getAppArtifactsByInvalidId() throws Exception {
        mvc.perform(get(APPS_VERSION + "/-1/artifacts").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getArtifactFilesByAppIdArtifactId() throws Exception {
        Application application = this.createTestApplication();
        Artifact artifact = application.getArtifacts().stream()
            .filter(a -> a.getType().getCode().equalsIgnoreCase(ArtifactType.HELM.getCode()))
            .findFirst().get();

        final String helloText = "Hello World!";
        final ByteArrayResource resource = new ByteArrayResource(helloText.getBytes());
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
        mockRestServiceServer.expect(requestTo(
                        helmUrl + "/" + application.getName() + "_" + application.getVersion() + "/charts/" + artifact.getName() + "-" + artifact.getVersion() + ".tgz"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(resource, MediaType.APPLICATION_OCTET_STREAM));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + "/artifacts/" + artifact.getId() + "/file").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getArtifactImageFilesByAppIdArtifactId() throws Exception {
        Application application = this.createTestApplication();
        Artifact artifact = application.getArtifacts().stream()
            .filter(a -> a.getType().getCode().equalsIgnoreCase(ArtifactType.IMAGE.getCode()))
            .findFirst().get();

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + "/artifacts/" + artifact.getId() + "/file").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError());
    }

    @Test
    public void getArtifactFilesByDifferentAppIdArtifactId() throws Exception {
        Application application = this.createTestApplication();
        Application application2 = this.createTestApplication();
        Artifact artifact = application.getArtifacts().stream()
            .filter(a -> a.getType().getCode().equalsIgnoreCase(ArtifactType.IMAGE.getCode()))
            .findFirst().get();

        mvc.perform(get(APPS_VERSION + "/" + application2.getId() + "/artifacts/" + artifact.getId() + "/file").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
    }

    @Test
    public void getArtifactWhenApplicationNotReady() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.PARSED, ApplicationMode.DISABLED);
        Artifact artifact = application.getArtifacts().stream()
            .filter(a -> a.getType().getCode().equalsIgnoreCase(ArtifactType.IMAGE.getCode()))
            .findFirst().get();

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + "/artifacts/" + artifact.getId() + "/file").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
    }

    @Test
    public void onBoardingApps_invalidFileExtension_returnStatusBadRequest() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        mvc.perform(multipart(APPS_VERSION).file(file)).andExpect(status().isBadRequest());
    }

    @Test
    public void onBoardingApps() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_NAME, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        when(environmentVariableCreator.createValueFromEnvVar("LOGS_STREAMING_METHOD", "indirect")).thenReturn("indirect");
        when(environmentVariableCreator.createEnvVarFromEnv(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvVarFromSecret(anyString(), anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvVarFromString(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvFromValue(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        MvcResult result = mvc.perform(multipart(APPS_VERSION).file(file)).andReturn();
        Assert.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(FILE_NAME));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
    }

    @Test
    public void onBoardingApps_logsDirectStreaming() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", FILE_NAME, MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        when(environmentVariableCreator.createValueFromEnvVar("LOGS_STREAMING_METHOD", "indirect")).thenReturn("direct");
        when(environmentVariableCreator.createEnvVarFromEnv(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvVarFromSecret(anyString(), anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvVarFromString(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvFromValue(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        MvcResult result = mvc.perform(multipart(APPS_VERSION).file(file)).andReturn();
        Assert.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(FILE_NAME));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
    }

    @Test
    public void onBoardingApps_filenameWithWhiteSpaces() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "hello world.csar", MediaType.MULTIPART_FORM_DATA_VALUE,
            "Hello World!".getBytes());
        when(environmentVariableCreator.createValueFromEnvVar("LOGS_STREAMING_METHOD", "indirect")).thenReturn("indirect");
        when(environmentVariableCreator.createEnvVarFromEnv(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvVarFromSecret(anyString(), anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvVarFromString(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        when(environmentVariableCreator.createEnvFromValue(anyString(), anyString())).thenReturn(new EnvVarBuilder().withName(ENV_VAR_NAME).withValue(ENV_VAR_VALUE).build());
        MvcResult result = mvc.perform(multipart(APPS_VERSION).file(file)).andReturn();
        Assert.assertEquals(HttpStatus.ACCEPTED.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("hello world.csar"));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
    }

    @Test
    public void onBoardingApps_invalidFileEnding_returnStatusBadRequest() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "hellocsar", MediaType.MULTIPART_FORM_DATA_VALUE, "Hello World!".getBytes());
        mvc.perform(multipart(APPS_VERSION).file(file)).andExpect(status().isBadRequest());
    }

    @Test
    public void updateAppModeToEnabledOnboardedApp() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}"))
            .andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.ENABLED)));
    }

    @Test
    public void updateAppModeToEnabled_invalidId_returnNotFound() throws Exception {
        mvc.perform(put(APPS_VERSION + "/100001").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateAppModeToEnabled_appNotOnboarded_returnBadRequest() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.FAILED, ApplicationMode.DISABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void updateAppModeToEnabled_invalidMode_returnBadRequest() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"TEST\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void updateAppModeToEnabled_nullMode_returnBadRequest() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"\"}")).andExpect(status().isBadRequest());
    }

    @Test
    public void updateAppModeToEnabled_alreadyEnabled_returnOkStatus() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.ENABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}")).andExpect(status().isOk());
    }

    @Test
    public void updateAppModeToDisabledOnboardedApp() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.ENABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"DISABLED\"}")).andExpect(status().isOk());
    }

    @Test
    public void updateAppModeToDisabled_alreadyDisabled_returnOkStatus() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"DISABLED\"}")).andExpect(status().isOk());
    }

    @Test
    public void updateAppStatusToDeletingOnboardedApp_returnOkStatus() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELETING\"}"))
            .andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationStatus.DELETING)));
    }

    @Test
    public void updateAppStatusToDeleting_NotOnboardedApp_returnBadRequest() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.PARSED, ApplicationMode.DISABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELETING\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void updateAppStatusToDeletingOnboardedApp_NotDisabled_returnBadRequest() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.ENABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELETING\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void updateAppStatusOnboardedApp_InvalidValue() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PARSING\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void updateAppStatusToDeletingFailedApp_returnOkStatus() throws Exception {
        Application application = this.createTestApplication(ApplicationStatus.FAILED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELETING\"}"))
            .andReturn();
        Assert.assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationStatus.DELETING)));
    }

    @Test
    public void multipleAppsOnboardingParallelEnable() {
        Application application1 = this.createTestApplication();
        Application application2 = this.createTestApplication();
        Application application3 = this.createTestApplication();

        LongStream.of(application1.getId(), application2.getId(), application3.getId()).parallel().forEach(appId -> {
            try {
                MvcResult result = mvc.perform(
                    put(APPS_VERSION + "/" + appId).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}")).andReturn();
                Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.ENABLED)));
            } catch (Exception e) {
                Assert.fail();
            }
        });
    }

    @Test
    public void multipleAppsOnboardingParallelDisable() {
        Application application1 = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.ENABLED);
        Application application2 = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.ENABLED);
        Application application3 = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.ENABLED);

        LongStream.of(application1.getId(), application2.getId(), application3.getId()).parallel().forEach(appId -> {
            try {
                MvcResult result = mvc.perform(
                    put(APPS_VERSION + "/" + appId).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"DISABLED\"}")).andReturn();
                Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
            } catch (Exception e) {
                Assert.fail();
            }
        });
    }

    @Test
    public void oneAppEnableIdempotentOperation() {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);

        IntStream.range(1, 10).forEach(counter -> {
            try {
                MvcResult result = mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}"))
                    .andReturn();
                Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.ENABLED)));
            } catch (Exception e) {
                Assert.fail();
            }
        });
    }

    @Test
    public void oneAppDisableIdempotentOperation() {
        Application application = this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.ENABLED);

        IntStream.range(1, 10).forEach(counter -> {
            try {
                MvcResult result = mvc.perform(put(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"DISABLED\"}"))
                    .andReturn();
                Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(String.valueOf(ApplicationMode.DISABLED)));
            } catch (Exception e) {
                Assert.fail();
            }
        });
    }

    public List<Application> sortBy(List<Application> unorderedList, String orderBy) {
        List<Application> sortList = unorderedList;
        Comparator<Application> idComparator = new Comparator<Application>() {
            @Override
            public int compare(Application application1, Application application2) {
                return application1.getId().compareTo(application2.getId());
            }
        };

        Comparator<Application> nameComparator = new Comparator<Application>() {
            @Override
            public int compare(Application application1, Application application2) {
                return application1.getName().compareTo(application2.getName());
            }
        };

        Comparator<Application> vendorComparator = new Comparator<Application>() {
            @Override
            public int compare(Application application1, Application application2) {
                return (application1.getVendor() == null || application2.getVendor() == null ? 0 : application1.getVendor().compareTo(application2.getVendor()));
            }
        };
        Comparator<Application> versionComparator = new Comparator<Application>() {
            @Override
            public int compare(Application application1, Application application2) {
                return application1.getVersion().compareTo(application2.getVersion());
            }
        };

        switch (orderBy) {
            case "name":
                sortList.sort(nameComparator);
                return sortList;
            case "id":
                sortList.sort(idComparator);
                return sortList;
            case "vendor":
                sortList.sort(vendorComparator);
                return sortList;
            case "version":
                sortList.sort(versionComparator);
                return sortList;
            default:
                return sortList;
        }
    }

    private Application createTestApplication() {
        return this.createTestApplication(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
    }

    private Application createTestApplication(ApplicationStatus status, ApplicationMode mode) {
        Application application = new Application();
        application.setStatus(status);
        application.setName("eric-oss-app-mgr");
        application.setVersion("0.0.0-1");
        application.setMode(mode);
        application.setVendor("Ericsson");
        application.setArtifacts(Set.of(
            this.getTestArtifact(application, "eric-oss-app-mgr", "0.0.0-1", ArtifactType.HELM),
            this.getTestArtifact(application, "eric-eo-api-gateway", "3.0.0-h217b265", ArtifactType.IMAGE),
            this.getTestArtifact(application, "hello-world-image-1", "3.0.0-1", ArtifactType.IMAGE)
        ));
        return applicationRepository.save(application);
    }

    private Artifact getTestArtifact(Application application, String name, String vertion, ArtifactType type) {
        final Artifact artifact1 = new Artifact();
        artifact1.setApplication(application);
        artifact1.setVersion(vertion);
        artifact1.setName(name);
        artifact1.setType(type);
        artifact1.setStatus(ArtifactStatus.COMPLETED);
        return  artifact1;
    }
}