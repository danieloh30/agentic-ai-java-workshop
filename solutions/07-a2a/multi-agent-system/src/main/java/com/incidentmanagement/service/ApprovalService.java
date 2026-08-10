package com.incidentmanagement.service;

import com.incidentmanagement.model.ApprovalProposal;
import com.incidentmanagement.model.ApprovalProposal.ApprovalStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import static jakarta.transaction.Transactional.TxType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for managing approval proposals and the Human-in-the-Loop workflow.
 * This service handles the async nature of human approvals - creating proposals,
 * waiting for human decisions, and continuing workflow execution.
 */
@ApplicationScoped
public class ApprovalService {

    @Inject
    EntityManager entityManager;

    /**
     * Map to store CompletableFutures waiting for approval decisions.
     * Key: incidentNumber, Value: CompletableFuture that completes when decision is made
     */
    private final Map<Integer, CompletableFuture<ApprovalProposal>> pendingApprovals = new ConcurrentHashMap<>();

    /**
     * Executor for async proposal creation to ensure transaction commits before blocking
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Create a new approval proposal and return a CompletableFuture that will complete
     * when a human makes a decision.
     */
    public CompletableFuture<ApprovalProposal> createProposalAndWaitForDecision(
            Integer incidentNumber,
            String incidentSystem,
            String incidentService,
            String incidentPriority,
            String businessImpact,
            String proposedEscalation,
            String escalationReason,
            String incidentDescription,
            String incidentReport) {

        // Check if there's already a pending proposal for this incident
        ApprovalProposal existing = ApprovalProposal.findPendingByIncidentNumber(incidentNumber);
        if (existing != null) {
            Log.warnf("Proposal already exists for incident %d, returning existing future", incidentNumber);
            return pendingApprovals.computeIfAbsent(incidentNumber, k -> new CompletableFuture<>());
        }

        // Create CompletableFuture first
        CompletableFuture<ApprovalProposal> future = new CompletableFuture<>();
        pendingApprovals.put(incidentNumber, future);

        // Create proposal in separate thread with its own transaction
        executor.submit(() -> {
            try {
                createProposalInNewTransaction(incidentNumber, incidentSystem, incidentService, incidentPriority, businessImpact,
                        proposedEscalation, escalationReason, incidentDescription, incidentReport);
                Log.info("Proposal creation transaction committed - now visible to queries");
            } catch (Exception e) {
                Log.errorf(e, "Failed to create proposal for incident %d", incidentNumber);
                future.completeExceptionally(e);
                pendingApprovals.remove(incidentNumber);
            }
        });

        return future;
    }

    @Transactional(TxType.REQUIRES_NEW)
    void createProposalInNewTransaction(
            Integer incidentNumber,
            String incidentSystem,
            String incidentService,
            String incidentPriority,
            String businessImpact,
            String proposedEscalation,
            String escalationReason,
            String incidentDescription,
            String incidentReport) {

        // Create new proposal
        ApprovalProposal proposal = new ApprovalProposal();
        proposal.incidentNumber = incidentNumber;
        proposal.incidentSystem = incidentSystem;
        proposal.incidentService = incidentService;
        proposal.incidentPriority = incidentPriority;
        proposal.businessImpact = businessImpact;
        proposal.proposedEscalation = proposedEscalation;
        proposal.escalationReason = escalationReason;
        proposal.incidentDescription = incidentDescription;
        proposal.incidentReport = incidentReport;
        proposal.status = ApprovalStatus.PENDING;
        proposal.createdAt = LocalDateTime.now();

        proposal.persist();
        entityManager.flush();

        Log.infof("Created approval proposal ID=%d for incident %d - %s / %s [%s] (Impact: %s, Proposed: %s)",
                proposal.id, incidentNumber, incidentSystem, incidentService, incidentPriority, businessImpact, proposedEscalation);
        Log.info("WORKFLOW PAUSED - Waiting for human approval decision");
        Log.infof("Proposal persisted with ID: %d, status: %s", proposal.id, proposal.status);
    }

    /**
     * Process a human's approval decision and complete the waiting CompletableFuture.
     * This resumes the workflow execution.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public ApprovalProposal processDecision(Integer proposalId, boolean approved, String reason, String approvedBy) {
        ApprovalProposal proposal = ApprovalProposal.findById(proposalId);
        if (proposal == null) {
            throw new IllegalArgumentException("Proposal not found: " + proposalId);
        }

        if (proposal.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Proposal is not pending: " + proposalId);
        }

        // Update proposal with decision
        proposal.status = approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
        proposal.decision = approved ? "APPROVED" : "REJECTED";
        proposal.approvalReason = reason;
        proposal.approvedBy = approvedBy;
        proposal.decidedAt = LocalDateTime.now();

        proposal.persist();

        Log.infof("Human decision received for incident %d: %s - %s",
                proposal.incidentNumber, proposal.decision, reason);
        Log.info("WORKFLOW RESUMED - Continuing with approval decision");

        // Complete the CompletableFuture to resume workflow
        CompletableFuture<ApprovalProposal> future = pendingApprovals.remove(proposal.incidentNumber);
        if (future != null) {
            future.complete(proposal);
        }

        return proposal;
    }

    /**
     * Get all pending approval proposals.
     */
    public List<ApprovalProposal> getPendingProposals() {
        return ApprovalProposal.findAllPending();
    }

    /**
     * Get a specific proposal by ID.
     */
    public ApprovalProposal getProposal(Integer proposalId) {
        return ApprovalProposal.findById(proposalId);
    }

    /**
     * Check if there's a pending approval for an incident.
     */
    public ApprovalProposal getPendingProposalForIncident(Integer incidentNumber) {
        return ApprovalProposal.findPendingByIncidentNumber(incidentNumber);
    }
}
