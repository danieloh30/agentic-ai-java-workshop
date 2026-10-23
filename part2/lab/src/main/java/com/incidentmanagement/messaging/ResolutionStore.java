package com.incidentmanagement.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentmanagement.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import java.util.*;

/** Registration, correlation, duplicate protection and latest-result queries are supplied. */
@ApplicationScoped
public class ResolutionStore {
    @Inject ObjectMapper mapper;

    @Transactional
    public IncidentEvent register(Integer incidentId, String report) {
        // Serialize publications for this incident and allocate the ordering key under the lock.
        IncidentInfo incident = IncidentInfo.findById(incidentId, LockModeType.PESSIMISTIC_WRITE);
        if (incident == null) throw new NotFoundException("Incident not found");
        IncidentEvent event = new IncidentEvent(UUID.randomUUID().toString(), incidentId, incident.system,
                incident.service, incident.priority, incident.description, report == null ? "" : report);
        ProcessingRequest row = new ProcessingRequest();
        row.requestId = event.requestId(); row.incidentId = incidentId; row.processingStatus = "PUBLISHED";
        try { row.inputJson = mapper.writeValueAsString(event); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
        row.persistAndFlush();
        if (incident.status != IncidentStatus.RESOLVED && incident.status != IncidentStatus.ESCALATED)
            incident.status = IncidentStatus.IN_PROGRESS;
        return event;
    }

    @Transactional
    public IncidentEvent input(String requestId) {
        ProcessingRequest row = find(requestId);
        if (row == null) throw new NotFoundException("Unknown request");
        try { return mapper.readValue(row.inputJson, IncidentEvent.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    @Transactional
    public ResolutionEvent completed(String requestId) {
        ProcessingRequest row = find(requestId);
        return row != null && terminal(row.processingStatus) ? row.outcome() : null;
    }

    @Transactional
    public void record(ResolutionEvent event) {
        if (event == null || event.requestId() == null || event.incidentId() == null
                || !terminal(event.processingStatus()) || event.resolution() == null || event.resolution().isBlank())
            throw new IllegalArgumentException("Invalid processing outcome");
        ProcessingRequest row = ProcessingRequest.find("requestId", event.requestId())
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (row == null || !row.incidentId.equals(event.incidentId()))
            throw new IllegalArgumentException("Uncorrelated processing outcome");
        if (terminal(row.processingStatus)) return; // First terminal outcome wins, including redelivery.
        row.processingStatus = event.processingStatus();
        row.triage = event.triage(); row.resolution = event.resolution();
        // Do not close incidents or overwrite their descriptions. An operator must verify recovery.
    }

    @Transactional
    public ResolutionEvent get(Integer incidentId) {
        ProcessingRequest row = ProcessingRequest.find("incidentId = ?1 order by id desc", incidentId).firstResult();
        return row == null ? null : row.outcome();
    }

    @Transactional
    public Collection<ResolutionEvent> all() {
        return ProcessingRequest.<ProcessingRequest>listAll().stream().map(ProcessingRequest::outcome).toList();
    }

    @Transactional
    public long count(String requestId) {
        return ProcessingRequest.count("requestId", requestId);
    }

    private static ProcessingRequest find(String requestId) {
        return ProcessingRequest.find("requestId", requestId).firstResult();
    }
    private static boolean terminal(String status) {
        return "AWAITING_REVIEW".equals(status) || "FAILED".equals(status);
    }
}
