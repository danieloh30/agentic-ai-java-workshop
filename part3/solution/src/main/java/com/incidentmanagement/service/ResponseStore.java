package com.incidentmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentmanagement.model.*;
import com.incidentmanagement.policy.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** All transitions use short transactions; no model call holds a database lock. */
@ApplicationScoped
public class ResponseStore {
    @Inject ObjectMapper mapper;

    @Transactional
    public ResponseRequest create(String id, Long incidentId, String report) {
        if (id == null
                || !id.matches("[a-zA-Z0-9-]{1,80}")
                || report == null
                || report.isBlank()
                || report.length() > 8000)
            throw new WebApplicationException(
                    "Request ID and report (1–8000 characters) required", 400);
        IncidentInfo incident = IncidentInfo.findById(incidentId);
        if (incident == null) throw new WebApplicationException(404);
        // Serialize request creation per incident, including concurrent duplicate submissions.
        IncidentInfo.findById(incidentId, LockModeType.PESSIMISTIC_WRITE);
        ResponseRequest existing = ResponseRequest.findById(id);
        if (existing != null) {
            if (!Objects.equals(existing.incidentId, incidentId) || !existing.report.equals(report))
                throw new WebApplicationException(
                        "Request ID already belongs to a different payload", 409);
            return existing;
        }
        var r = new ResponseRequest();
        r.id = id;
        r.incidentId = incidentId;
        r.priority = incident.priority;
        r.report = report;
        r.event("QUEUED");
        r.persist();
        return r;
    }

    @Transactional
    public ResponseRequest claim() {
        ResponseRequest r =
                ResponseRequest.find("status = ?1 order by updatedAt", "QUEUED")
                        .withLock(LockModeType.PESSIMISTIC_WRITE)
                        .firstResult();
        if (r != null) {
            r.attempts++;
            r.event("PLANNING");
        }
        return r;
    }

    @Transactional
    public void complete(String id, int attempt, Recommendation recommendation) {
        var r = locked(id);
        if (!"PLANNING".equals(r.status) || r.attempts != attempt) return;
        if (!RecommendationChecks.violations(recommendation).isEmpty()
                || !RecommendationChecks.grounded(recommendation, r.report)) {
            r.event("FAILED");
            return;
        }
        try {
            r.proposal = mapper.writeValueAsString(recommendation);
            r.proposalHash =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(r.proposal.getBytes(StandardCharsets.UTF_8)));
            r.event("WAITING_APPROVAL");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void fail(String id, int attempt) {
        var r = locked(id);
        if ("PLANNING".equals(r.status) && r.attempts == attempt) r.event("FAILED");
    }

    @Transactional
    public ResponseRequest decide(String id, String hash, boolean approve) {
        var r = locked(id);
        if (hash == null || !hash.equals(r.proposalHash))
            throw new WebApplicationException("Proposal hash mismatch", 409);
        String target = approve ? "APPROVED" : "REJECTED";
        if (target.equals(r.status)) return r;
        if (!"WAITING_APPROVAL".equals(r.status))
            throw new WebApplicationException("Request is not awaiting this decision", 409);
        r.event(target);
        return r;
    }

    @Transactional
    public ResponseRequest retry(String id) {
        var r = locked(id);
        if (!List.of("FAILED", "INTERRUPTED").contains(r.status))
            throw new WebApplicationException(
                    "Only failed/interrupted planning may be retried", 409);
        r.event("QUEUED");
        return r;
    }

    @Transactional
    public void recover(Instant cutoff) {
        for (ResponseRequest r : ResponseRequest.<ResponseRequest>list("status", "PLANNING")) {
            r = locked(r.id);
            if (RecoveryPolicy.abandoned(r.status, r.updatedAt, cutoff)) r.event("INTERRUPTED");
        }
    }

    private ResponseRequest locked(String id) {
        ResponseRequest r = ResponseRequest.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (r == null) throw new WebApplicationException(404);
        return r;
    }

    @Transactional
    public List<ResponseRequest> all() {
        return ResponseRequest.list("order by updatedAt desc");
    }

    @Transactional
    public ResponseRequest get(String id) {
        var r = (ResponseRequest) ResponseRequest.findById(id);
        if (r == null) throw new WebApplicationException(404);
        return r;
    }
}
