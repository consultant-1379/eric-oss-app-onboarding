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

package com.ericsson.oss.ae.apponboarding.v1.controller.delete;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v1.TestKubernetesServerConfig;
import com.ericsson.oss.ae.apponboarding.v1.controller.ApplicationController;
import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.*;
import com.ericsson.oss.ae.apponboarding.v1.service.jobs.JobsEventHandler;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, ApplicationController.class })
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@ContextConfiguration(classes = TestKubernetesServerConfig.class)
public class DeletePackageIntegrationTest {

    private static boolean DB_OBJECTS_CREATED = false;
    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mvc;
    private static final String APPS_VERSION = Consts.API_V1_APPS;
    private static final String ARTIFACTS = "/artifacts/";
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ArtifactRepository artifactRepository;

    @Qualifier("helmRegistry")
    @MockBean
    private RestTemplate helmRestTemplate;

    @Qualifier("imageRegistry")
    @MockBean
    private RestTemplate imageRestTemplate;

    @SpyBean JobsEventHandler jobsEventHandler;

    @PostConstruct
    public void postConstruct() {
    }

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void uploaded_status_package_cronjob_delete() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.UPLOADED, ApplicationMode.DISABLED);

        MvcResult result = mvc.perform(get(APPS_VERSION).contentType(MediaType.APPLICATION_JSON)).andReturn();

        await().atMost(8000, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            verify(jobsEventHandler, times(4)).performScheduledTask();
        });

        mvc.perform(get(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_image_get_manifest_fail_conflict() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation/api");

        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.CONFLICT));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
    }

    @Test
    public void deleteApp_image_get_manifest_fail_unauthorized() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation1/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.UNAUTHORIZED));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteApp_image_get_manifest_fail_bad_request() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation2/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.BAD_REQUEST));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    public void deleteApp_image_get_manifest_fail_forbidden() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation3/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.FORBIDDEN));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void deleteApp_image_get_manifest_fail_too_many_requests() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation4/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.TOO_MANY_REQUESTS));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isTooManyRequests());
    }

    @Test
    public void deleteApp_image_get_manifest_fail_method_not_allowed() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation36/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.METHOD_NOT_ALLOWED));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void deleteApp_image_get_manifest_success_not_found() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation5/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.NOT_FOUND));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    public void deleteApp_image_get_manifest_fail_gateway_timeout() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation6/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getServerException(HttpStatus.GATEWAY_TIMEOUT));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isGatewayTimeout());
    }

    @Test
    public void deleteApp_image_delete_fail_not_supported() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation7/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenThrow(getServerException(HttpStatus.NOT_IMPLEMENTED));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotImplemented());
        Mockito.verify(imageRestTemplate, times(3))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
    }

    @Test
    public void deleteApp_image_get_manifest_fail_not_acceptable() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation32/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntityForDockerContentDigestFailure(HttpStatus.NOT_ACCEPTABLE));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable());
    }

    @Test
    public void deleteApp_image_delete_success_not_found() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation8/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.NOT_FOUND));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    public void deleteApp_image_delete_fail_conflict() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation9/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.CONFLICT));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
    }

    @Test
    public void deleteApp_image_delete_fail_gateway_timeout() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation10/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenThrow(getServerException(HttpStatus.GATEWAY_TIMEOUT));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isGatewayTimeout());
        Mockito.verify(imageRestTemplate, times(3))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
    }

    @Test
    public void deleteApp_image_get_manifest_success_two_same_requests() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation11/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));

        MvcResult result1 = mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andReturn();
        MvcResult result2 = mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andReturn();
        Assert.assertTrue(
            result1.getResponse().getStatus() == HttpStatus.NOT_FOUND.value() || result2.getResponse().getStatus() == HttpStatus.NOT_FOUND.value());
        Assert.assertTrue(
            result1.getResponse().getStatus() == HttpStatus.NO_CONTENT.value() || result2.getResponse().getStatus() == HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void deleteApp_helm_delete_success_not_found() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation12/api");
        Mockito.doThrow(getClientError(HttpStatus.NOT_FOUND)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    public void deleteApp_helm_delete_fail_unauthorized() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation13/api");
        Mockito.doThrow(getClientError(HttpStatus.UNAUTHORIZED)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteApp_helm_delete_fail_bad_request() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation14/api");
        Mockito.doThrow(getClientError(HttpStatus.BAD_REQUEST)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    public void deleteApp_helm_delete_fail_forbidden() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation15/api");
        Mockito.doThrow(getClientError(HttpStatus.FORBIDDEN)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void deleteApp_helm_delete_fail_too_many_requests() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation16/api");
        Mockito.doThrow(getClientError(HttpStatus.TOO_MANY_REQUESTS)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isTooManyRequests());
    }

    @Test
    public void deleteApp_helm_delete_fail_method_not_allowed() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation17/api");
        Mockito.doThrow(getClientError(HttpStatus.METHOD_NOT_ALLOWED)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void deleteApp_helm_delete_fail_method_gateway_timeout() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation18/api");
        Mockito.doThrow(getServerException(HttpStatus.GATEWAY_TIMEOUT)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isGatewayTimeout());
    }

    @Test
    public void deleteApp_helm_delete_fail_method_not_implemented() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation19/api");
        Mockito.doThrow(getClientError(HttpStatus.NOT_IMPLEMENTED)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotImplemented());
    }

    @Test
    public void deleteApp_helm_delete_fail_method_conflict() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation20/api");
        Mockito.doThrow(getClientError(HttpStatus.CONFLICT)).when(helmRestTemplate).delete(Mockito.anyString());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
    }

    @Test
    public void deleteApp_id_NotFound() throws Exception {
        mvc.perform(delete(APPS_VERSION + "/" + "141414").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_status_not_deleting() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.PARSED, ApplicationMode.DISABLED);
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    public void deleteApp_mode_not_disabled() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.ENABLED);
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    public void deleteApp_single_image_artifact_successful() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation21/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.NO_CONTENT));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        Mockito.verify(imageRestTemplate, times(1))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_single_image_artifact_successful_with_servicemesh() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation35/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntityWithServiceMeshEnabled(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntityWithServiceMeshEnabled(HttpStatus.NO_CONTENT));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        Mockito.verify(imageRestTemplate, times(1))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_single_helm_artifact_successful() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact = createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation22/api");
        Mockito.when(helmRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(helmRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.NO_CONTENT));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        Mockito.verify(helmRestTemplate, times(1)).delete(anyString());
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_multiple_artifacts_successful() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact1 = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation23/api");
        Artifact artifact2 = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation24/api");

        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.NO_CONTENT));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact1.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact2.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        Mockito.verify(imageRestTemplate, times(2))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact1.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact2.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_artifact_not_found_registry_exists_in_db() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation25/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.NOT_FOUND));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        Mockito.verify(imageRestTemplate, times(0))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_artifact_location_shared_delete_one() throws Exception {
        Application application1 = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Application application2 = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact1 = createArtifactForTests(application1, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation26/api");
        Artifact artifact2 = createArtifactForTests(application2, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation26/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.NO_CONTENT));

        mvc.perform(get(APPS_VERSION + "/" + application1.getId() + ARTIFACTS + artifact1.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(get(APPS_VERSION + "/" + application2.getId() + ARTIFACTS + artifact2.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(delete(APPS_VERSION + "/" + application1.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        Mockito.verify(imageRestTemplate, times(0))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
        mvc.perform(get(APPS_VERSION + "/" + application1.getId() + ARTIFACTS + artifact1.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
        mvc.perform(get(APPS_VERSION + "/" + application2.getId() + ARTIFACTS + artifact2.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void deleteApp_same_location_twice_single_App() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact1 = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation27/api");
        Artifact artifact2 = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation27/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.NO_CONTENT));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact1.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact2.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        Mockito.verify(imageRestTemplate, times(1))
            .exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null), ArgumentMatchers.eq(String.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact1.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact2.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteApp_success_request_invalid_locations() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, null);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));

        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    public void deleteApp_success_request_event_permission_exist() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation28/api");
        Application applicationWithEvents = createApplicationEventForTests(application);
        Application applicationWithEventsPermissions = createPermissionForTests(applicationWithEvents);
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));

        MvcResult result1 = mvc.perform(delete(APPS_VERSION + "/" + applicationWithEventsPermissions.getId()).contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        MvcResult result2 = mvc.perform(delete(APPS_VERSION + "/" + applicationWithEventsPermissions.getId()).contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        Assert.assertTrue(
            result1.getResponse().getStatus() == HttpStatus.NOT_FOUND.value() || result2.getResponse().getStatus() == HttpStatus.NOT_FOUND.value());
        Assert.assertTrue(
            result1.getResponse().getStatus() == HttpStatus.NO_CONTENT.value() || result2.getResponse().getStatus() == HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void deleteApp_success_request_mixed_artifact_types() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation29/api");
        createArtifactForTests(application, ArtifactType.HELM, ArtifactStatus.COMPLETED, "UniqueLocation30/api");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));
        Mockito.when(helmRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.DELETE), ArgumentMatchers.eq(null),
            ArgumentMatchers.eq(String.class))).thenReturn(getResponseEntity(HttpStatus.OK));

        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    public void deleteFailedApp_failedArtifact_notInRegistry_existsInDb() throws Exception {
        Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        Artifact artifact = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.FAILED,
            "32/OtherDefinitions/ASD/Images/docker.tar");
        Mockito.when(imageRestTemplate.exchange(Mockito.anyString(), ArgumentMatchers.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
            ArgumentMatchers.eq(String.class))).thenThrow(getClientError(HttpStatus.NOT_FOUND));

        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + artifact.getId() + "/file").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict());
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    private ResponseEntity<String> getResponseEntity(HttpStatus requiredResponseCode) {
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        header.set("Docker-Content-Digest", "sha:111212");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("some response body", header, requiredResponseCode);
        return responseEntity;
    }

    private ResponseEntity<String> getResponseEntityWithServiceMeshEnabled(HttpStatus requiredResponseCode) {
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        header.set("docker-content-digest", "sha:111212");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("some response body", header, requiredResponseCode);
        return responseEntity;
    }

    private ResponseEntity<String> getResponseEntityForDockerContentDigestFailure(HttpStatus requiredResponseCode) {
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        header.set("docker-Content-Digest-Test-failure", "sha:111212");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("some response body", header, requiredResponseCode);
        return responseEntity;
    }

    private HttpClientErrorException getClientError(HttpStatus requiredResponseCode) {
        HttpClientErrorException error = new HttpClientErrorException(requiredResponseCode);
        return error;
    }

    private HttpServerErrorException getServerException(HttpStatus requiredResponseCode) {
        HttpServerErrorException error = new HttpServerErrorException(requiredResponseCode);
        return error;
    }

    private Application createApplicationForTests(ApplicationStatus status, ApplicationMode mode) {
        final Application application = new Application();
        application.setStatus(status);
        application.setName("eric-oss-app-mgr");
        application.setVersion("0.0.0-1");
        application.setMode(mode);
        application.setOnboardedDate(new Date(System.currentTimeMillis() - (60 * 60 + 5) * 1000));
        return applicationRepository.save(application);
    }

    private Artifact createArtifactForTests(Application application, ArtifactType type, ArtifactStatus status, String location) {
        final Artifact artifact = new Artifact();
        artifact.setApplication(application);
        artifact.setVersion("0.0.0-1");
        artifact.setName("eric-oss-app-mgr");
        artifact.setType(type);
        artifact.setStatus(status);
        artifact.setLocation(location);
        return artifactRepository.save(artifact);
    }

    private Application createApplicationEventForTests(Application application) {
        ApplicationEvent event = new ApplicationEvent();
        event.setDate(new java.util.Date());
        event.setApplication(application);
        event.setText("Application Event");
        Set<ApplicationEvent> appEventSet = new HashSet<>();
        appEventSet.add(event);
        application.setEvents(appEventSet);
        return applicationRepository.save(application);
    }

    private Application createPermissionForTests(Application application) {
        Permission permission = new Permission();
        permission.setApplication(application);
        Set<Permission> permissionSet = new HashSet<>();
        permissionSet.add(permission);
        application.setPermissions(permissionSet);
        return applicationRepository.save(application);
    }
}