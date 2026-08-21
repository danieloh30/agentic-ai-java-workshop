package com.incidentmanagement.resource;

import com.incidentmanagement.model.ApprovalProposal;
import com.incidentmanagement.service.ApprovalService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;
import java.util.Map;

@Path("/api/approvals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApprovalResource {

    @Inject
    ApprovalService approvalService;

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> mapIllegalArgument(IllegalArgumentException e) {
        return RestResponse.status(Response.Status.NOT_FOUND, Map.of("error", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> mapIllegalState(IllegalStateException e) {
        return RestResponse.status(Response.Status.BAD_REQUEST, Map.of("error", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> mapGeneral(Exception e) {
        Log.error("Error processing request", e);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                Map.of("error", "Error processing: " + e.getMessage()));
    }

    @GET
    @Path("/pending")
    public List<ApprovalProposal> getPendingProposals() {
        return approvalService.getPendingProposals();
    }

    @GET
    @Path("/{proposalId}")
    public ApprovalProposal getProposal(Integer proposalId) {
        ApprovalProposal proposal = approvalService.getProposal(proposalId);
        if (proposal == null) {
            throw new NotFoundException("Proposal not found");
        }
        return proposal;
    }

    @POST
    @Path("/{proposalId}/approve")
    public ApprovalProposal approveProposal(Integer proposalId, Map<String, String> request) {
        String reason = request.getOrDefault("reason", "Approved by human reviewer");
        String approvedBy = request.getOrDefault("approvedBy", "Workshop User");

        Log.infof("Approval request received for proposal %d by %s", proposalId, approvedBy);

        return approvalService.processDecision(proposalId, true, reason, approvedBy);
    }

    @POST
    @Path("/{proposalId}/reject")
    public ApprovalProposal rejectProposal(Integer proposalId, Map<String, String> request) {
        String reason = request.getOrDefault("reason", "Rejected by human reviewer");
        String approvedBy = request.getOrDefault("approvedBy", "Workshop User");

        Log.infof("Rejection request received for proposal %d by %s", proposalId, approvedBy);

        return approvalService.processDecision(proposalId, false, reason, approvedBy);
    }

    @POST
    @Path("/{proposalId}/decide")
    public ApprovalProposal decideProposal(Integer proposalId, Map<String, String> request) {
        String decision = request.get("decision");
        String reason = request.getOrDefault("reason", "Decision by human reviewer");
        String approvedBy = request.getOrDefault("approvedBy", "Workshop User");

        if (decision == null || (!decision.equals("KEEP_AT_TEAM") && !decision.equals("ESCALATE_INCIDENT"))) {
            throw new BadRequestException("Decision must be either KEEP_AT_TEAM or ESCALATE_INCIDENT");
        }

        Log.infof("Decision '%s' received for proposal %d by %s", decision, proposalId, approvedBy);

        String fullReason = decision + ": " + reason;

        return approvalService.processDecision(proposalId, true, fullReason, approvedBy);
    }
}
