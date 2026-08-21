package com.incidentmanagement.resource;

import com.incidentmanagement.service.HumanInputService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.logging.Log;

import java.util.Map;

@Path("/api/human-input")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HumanInputResource {

    @Inject
    HumanInputService humanInputService;

    @GET
    @Path("/pending")
    public Map<String, String> getPendingRequests() {
        return humanInputService.getPendingRequests();
    }

    @POST
    @Path("/{requestId}")
    public Map<String, String> provideInput(String requestId, Map<String, String> request) {
        String decision = request.get("decision");
        if (decision == null || decision.isBlank()) {
            throw new BadRequestException("Decision is required");
        }

        if (!humanInputService.hasPendingRequest(requestId)) {
            throw new NotFoundException("No pending request found for: " + requestId);
        }

        Log.infof("Human decision received for %s: %s", requestId, decision);
        humanInputService.provideInput(requestId, decision);

        return Map.of(
            "message", "Decision recorded",
            "requestId", requestId,
            "decision", decision
        );
    }
}
