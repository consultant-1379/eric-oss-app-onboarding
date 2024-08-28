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

package com.ericsson.oss.ae.apponboarding.v1.unit;

import com.ericsson.oss.ae.apponboarding.v1.dao.ApplicationRepository;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.Application;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationEvent;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationMode;
import com.ericsson.oss.ae.apponboarding.v1.model.entity.ApplicationStatus;
import com.ericsson.oss.ae.apponboarding.v1.service.ApplicationService;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class ApplicationServiceUnitTest {

    @InjectMocks private ApplicationService applicationService;

    @Mock private EntityManager entityManager;

    @Mock private ApplicationRepository applicationRepository;

    @Test
    public void testFindByName() {
        EntityGraph graph = Mockito.mock(EntityGraph.class);
        Long appId = 1l;
        Mockito.when(entityManager.getEntityGraph(Mockito.anyString())).thenReturn(graph);
        Mockito.when(entityManager.find(Mockito.any(), Mockito.anyLong(), Mockito.anyMap())).thenReturn(makeApplication(appId));
        Application application = applicationService.findApplicationEvents(appId);
        assertEquals(1, application.getEvents().size());
    }

    @Test
    public void testSaveApplication() {
        Long appId = 1l;
        Mockito.when(applicationRepository.save(Mockito.any(Application.class))).thenReturn(makeApplication(appId));
        applicationService.saveApplication(makeApplication(appId));
    }

    public Application makeApplication(Long id) {
        final Application application = new Application();
        application.setStatus(ApplicationStatus.ONBOARDED);
        application.setName("eric-oss-app-mgr");
        application.setVersion("0.0.0-1");
        application.setMode(ApplicationMode.DISABLED);
        application.setId(id);
        ApplicationEvent event = new ApplicationEvent();
        event.setText("Event Occurred - " + id);
        event.setApplication(application);
        event.setDate(new Date());
        Set<ApplicationEvent> events = new HashSet<>();
        events.add(event);
        application.setEvents(events);
        return application;
    }

}
