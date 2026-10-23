package com.incidentmanagement.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class ResponseRequest extends PanacheEntityBase {
    @Id public String id;
    public Long incidentId;
    public String priority;

    @Column(columnDefinition = "text")
    public String report;

    public String status;

    @Column(columnDefinition = "text")
    public String proposal;

    public String proposalHash;
    public Instant updatedAt;

    @Column(columnDefinition = "text")
    public String audit;

    public int attempts;

    public void event(String state) {
        status = state;
        updatedAt = Instant.now();
        audit = (audit == null ? "" : audit) + updatedAt + " " + state + "\n";
    }
}
