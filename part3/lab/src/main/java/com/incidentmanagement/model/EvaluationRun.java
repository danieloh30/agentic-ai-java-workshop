package com.incidentmanagement.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class EvaluationRun extends PanacheEntity {
    public String label;
    public String datasetHash;
    public String skillHash;
    public String rubric;
    public String model;
    public Instant createdAt;
    public long elapsedMillis;

    @Column(columnDefinition = "text")
    public String results;
}
