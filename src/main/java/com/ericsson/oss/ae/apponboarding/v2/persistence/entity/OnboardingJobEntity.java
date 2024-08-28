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

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.ericsson.oss.ae.apponboarding.api.v2.model.OnboardingJobStatus;
import com.ericsson.oss.ae.apponboarding.common.consts.Consts;
import jakarta.persistence.*;
import lombok.*;

/**
 * The persistent class for the onboarding_job database table.
 */
@Entity
@Table(name = "onboarding_job", schema = Consts.ONBOARDING_DB_V2_SCHEMA)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class OnboardingJobEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false, length = Consts.ENTITY_FIELD_LENGTH_36)
    private UUID id;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String appId;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String packageVersion;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String vendor;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String type;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String fileName;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String packageSize;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String appName;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_255) private String provider;

    @Column(length = Consts.ENTITY_FIELD_LENGTH_50)
    @Convert(converter = OnboardingJobStatusConverter.class)
    private OnboardingJobStatus status;

    @Builder.Default private Timestamp startTimestamp = Timestamp.from(Instant.now());

    private Timestamp endTimestamp;

    @OneToMany(mappedBy = "onboardingJobEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<OnboardingEventEntity> onboardingEventEntities;

}