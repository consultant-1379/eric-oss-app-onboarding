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

package com.ericsson.oss.ae.apponboarding.v1.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.Set;

@EqualsAndHashCode
@NamedEntityGraph(name = "application-events-entity-graph", attributeNodes = { @NamedAttributeNode("events"), })
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String username;
    private String version;
    private String size;
    private String vendor;
    private String type;
    private Date onboardedDate;
    private String descriptorInfo;
    private ApplicationStatus status;
    private ApplicationMode mode;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonManagedReference
    private Set<Artifact> artifacts;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonManagedReference
    private Set<Permission> permissions;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonManagedReference
    private Set<ApplicationEvent> events;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonManagedReference
    private Set<Role> roles;
}