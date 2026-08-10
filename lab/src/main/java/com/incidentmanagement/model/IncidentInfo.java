package com.incidentmanagement.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_info")
public class IncidentInfo extends PanacheEntity {

    @Column(name = "system_name")
    public String system;

    public String service;

    public Integer priority;

    public String description;

    @Enumerated(EnumType.STRING)
    public IncidentStatus status;
}
