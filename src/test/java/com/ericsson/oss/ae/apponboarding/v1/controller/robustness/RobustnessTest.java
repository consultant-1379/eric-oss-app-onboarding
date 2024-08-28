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

package com.ericsson.oss.ae.apponboarding.v1.controller.robustness;

import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.TEST_CSAR_LOCATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.ae.apponboarding.CoreApplication;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import com.ericsson.oss.ae.apponboarding.v1.TestKubernetesServerConfig;
import com.ericsson.oss.ae.apponboarding.v1.controller.ApplicationController;
import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.dao.ArtifactRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.*;
import com.ericsson.oss.ae.apponboarding.v1.service.ApplicationService;
import com.ericsson.oss.ae.apponboarding.v2.utils.FileUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CoreApplication.class, ApplicationController.class })
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@ContextConfiguration(classes = TestKubernetesServerConfig.class)
public class RobustnessTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;
    private static final String APPS_VERSION = Consts.API_V1_APPS;
    private static final String ARTIFACTS = "/artifacts";
    private static final String FILE = "/file";
    @Autowired
    private ApplicationService applicationService;
    @MockBean
    private ApplicationRepository applicationRepository;
    @MockBean
    private ArtifactRepository artifactRepository;
    @MockBean
    private Artifact artifact;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void deleteApp_find_by_id_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        when(applicationRepository.findById(Mockito.anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void deleteApp_find_artifact_by_location_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        final List<Artifact> artifacts = new ArrayList<>();
        when(applicationRepository.findById(Mockito.anyLong())).thenReturn(java.util.Optional.of(application));
        when(artifactRepository.findByLocationIn(Mockito.anyList())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void deleteApp_from_db_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.DELETING, ApplicationMode.DISABLED);
        final List<Artifact> artifacts = new ArrayList<>();
        when(applicationRepository.findById(Mockito.anyLong())).thenReturn(java.util.Optional.of(application));
        when(artifactRepository.findByLocationIn(Mockito.anyList())).thenReturn(artifacts);
        doThrow(Mockito.mock(CannotCreateTransactionException.class)).when(applicationRepository).delete(any(Application.class));
        mvc.perform(delete(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void findAll_Apps_throw_internal_server_error() throws Exception {
        when(applicationRepository.findAll()).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(get(APPS_VERSION).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError());
    }

    @Test
    public void findApp_by_id_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        when(applicationRepository.findById(anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getArtifact_by_app_id_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        when(applicationRepository.findById(anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getArtifact_by_artifact_id_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        final Artifact artifact = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation28/api");
        when(artifactRepository.findByApplicationIdAndId(anyLong(), anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + "/" + artifact.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getArtifactFile_by_artifact_id_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        final Artifact artifact = createArtifactForTests(application, ArtifactType.IMAGE, ArtifactStatus.COMPLETED, "UniqueLocation28/api");
        //Commented by EKMAUHR (Harish Kumar I) to support SonarQube as getById is deprecated in SpringBoot 2.7.x
        //when(artifactRepository.getById(anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        when(artifactRepository.getReferenceById(anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(get(APPS_VERSION + "/" + application.getId() + ARTIFACTS + "/" + artifact.getId() + FILE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testOnboardingJobs_DataAccessException_error() throws Exception {

        // Given
        final byte[] csarContent = FileUtils.readFileToBytes(TEST_CSAR_LOCATION);
        final MockMultipartFile file = new MockMultipartFile("file", "hellocsar.csar", MediaType.MULTIPART_FORM_DATA_VALUE,
                "Hello World!".getBytes());
        when(applicationRepository.save(Mockito.any())).thenThrow(Mockito.mock(DataAccessException.class));
        // When
        final MvcResult result = mvc.perform(multipart(APPS_VERSION).file(file)).andReturn();

        //Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());

    }

    @Test
    public void testOnboardingJobs_transaction_error() throws Exception {

        // Given
        final byte[] csarContent = FileUtils.readFileToBytes(TEST_CSAR_LOCATION);
        final MockMultipartFile file = new MockMultipartFile("file", "hellocsar.csar", MediaType.MULTIPART_FORM_DATA_VALUE,
                "Hello World!".getBytes());
        when(applicationRepository.save(Mockito.any())).thenThrow(Mockito.mock(TransactionException.class));
        // When
        final MvcResult result = mvc.perform(multipart(APPS_VERSION).file(file)).andReturn();

        //Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());

    }

    @Test
    public void onboard_csar_throw_internal_server_error() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "hellocsar.csar", MediaType.MULTIPART_FORM_DATA_VALUE,
                "Hello World!".getBytes());
        when(applicationRepository.save(any(Application.class))).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(multipart(APPS_VERSION).file(file)).andExpect(status().isInternalServerError());

    }

    @Test
    public void updateApplication_find_by_id_throw_internal_server_error() throws Exception {
        when(applicationRepository.findById(anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(put(APPS_VERSION + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    public void updateApplication_save_updated_app_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        when(applicationRepository.findById(anyLong())).thenReturn(java.util.Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(put(APPS_VERSION + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void updateApplication_throw_internal_server_error() throws Exception {
        final Application application = createApplicationForTests(ApplicationStatus.ONBOARDED, ApplicationMode.DISABLED);
        when(applicationRepository.findById(anyLong())).thenReturn(java.util.Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationRepository.findById(anyLong())).thenThrow(Mockito.mock(CannotCreateTransactionException.class));
        mvc.perform(put(APPS_VERSION + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"ENABLED\"}"))
                .andExpect(status().isInternalServerError());
    }

    private Application createApplicationForTests(final ApplicationStatus status, final ApplicationMode mode) {
        final Application application = new Application();
        application.setId(1L);
        application.setStatus(status);
        application.setName("eric-oss-app-mgr");
        application.setVersion("0.0.0-1");
        application.setMode(mode);
        application.setArtifacts(Collections.emptySet());
        return application;
    }

    private Artifact createArtifactForTests(final Application application, final ArtifactType type, final ArtifactStatus status, final String location) {
        final Artifact artifact = new Artifact();
        artifact.setId(1L);
        artifact.setApplication(application);
        artifact.setVersion("0.0.0-1");
        artifact.setName("eric-oss-app-mgr");
        artifact.setType(type);
        artifact.setStatus(status);
        artifact.setLocation(location);
        return artifact;
    }

}
