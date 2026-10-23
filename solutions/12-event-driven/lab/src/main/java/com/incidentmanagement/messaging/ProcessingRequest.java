package com.incidentmanagement.messaging;

import com.incidentmanagement.model.*;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/** One durable row per accepted publication, including its original payload for replay. */
@Entity
public class ProcessingRequest extends PanacheEntityBase {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "processing_request_seq")
    @SequenceGenerator(name = "processing_request_seq", sequenceName = "processing_request_seq", allocationSize = 1)
    public Long id;
    @Column(unique = true, nullable = false) public String requestId;
    @Column(nullable = false) public Integer incidentId;
    @Column(nullable = false) public String processingStatus;
    @Column(columnDefinition = "text", nullable = false) public String inputJson;
    @Column(columnDefinition = "text") public String triage;
    @Column(columnDefinition = "text") public String resolution;

    ResolutionEvent outcome() {
        return new ResolutionEvent(requestId, incidentId, processingStatus, triage, resolution);
    }
}
