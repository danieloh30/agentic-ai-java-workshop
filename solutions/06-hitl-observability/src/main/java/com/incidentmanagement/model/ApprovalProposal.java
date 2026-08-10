package com.incidentmanagement.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

/**
 * Entity representing an escalation proposal awaiting human approval.
 * This is the core of the Human-in-the-Loop pattern - proposals are stored
 * and the workflow pauses until a human makes an approval decision.
 */
@Entity
public class ApprovalProposal extends PanacheEntity {

    /**
     * The incident number this proposal is for
     */
    @Column(nullable = false)
    public Integer incidentNumber;

    /**
     * Affected system
     */
    public String incidentSystem;

    /**
     * Affected service
     */
    public String incidentService;

    /**
     * Incident priority
     */
    public String incidentPriority;

    @Column(columnDefinition = "TEXT")
    public String businessImpact;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String proposedEscalation;

    @Column(columnDefinition = "TEXT")
    public String escalationReason;

    @Column(columnDefinition = "TEXT")
    public String incidentDescription;

    @Column(columnDefinition = "TEXT")
    public String incidentReport;

    /**
     * Current status of the approval
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ApprovalStatus status = ApprovalStatus.PENDING;

    /**
     * Human's decision (APPROVED or REJECTED)
     */
    public String decision;

    /**
     * Human's reasoning for their decision
     */
    @Column(length = 1000)
    public String approvalReason;

    /**
     * Who approved/rejected (for audit trail)
     */
    public String approvedBy;

    /**
     * When the proposal was created
     */
    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    /**
     * When the decision was made
     */
    public LocalDateTime decidedAt;

    /**
     * Find pending proposal for a specific incident
     */
    public static ApprovalProposal findPendingByIncidentNumber(Integer incidentNumber) {
        return find("incidentNumber = ?1 and status = ?2", incidentNumber, ApprovalStatus.PENDING).firstResult();
    }

    /**
     * Find all pending proposals
     */
    public static java.util.List<ApprovalProposal> findAllPending() {
        return find("status", ApprovalStatus.PENDING).list();
    }

    /**
     * Approval status enum
     */
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
