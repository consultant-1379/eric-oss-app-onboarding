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

package com.ericsson.oss.ae.apponboarding.v2.persistence.entity;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingEventType;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * The persistent class for the onboarding_event database table.
 *
 */
@Entity
@Table(name="onboarding_event", schema = Consts.ONBOARDING_DB_V2_SCHEMA)
@EqualsAndHashCode
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class OnboardingEventEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique=true, nullable=false, length=Consts.ENTITY_FIELD_LENGTH_36)
    private UUID id;

    @Column(length=Consts.ENTITY_FIELD_LENGTH_50)
    @Convert(converter = OnboardingEventTypeConverter.class)
    private OnboardingEventType type;

    @Column(length=Consts.ENTITY_FIELD_LENGTH_255)
    private String title;

    @Column(length=Consts.ENTITY_FIELD_LENGTH_255)
    private String detail;

    @Builder.Default
    private Timestamp timestamp = Timestamp.from(Instant.now());

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = Consts.ONBOARDING_EVENT_COLUMN_ONBOARDING_JOB_ID)
    private OnboardingJobEntity onboardingJobEntity;

}