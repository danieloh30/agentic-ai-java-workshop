package com.incidentmanagement.resource;

import com.incidentmanagement.agentic.workflow.ConsensusPolicy;
import com.incidentmanagement.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** Workshop-only fixtures: no model calls, no incident writes. */
@Path("/incident-consensus/fixtures")
@Produces(MediaType.APPLICATION_JSON)
public class ConsensusFixtureResource {
    @GET @Path("/{scenario}")
    public ConsensusResult fixture(String scenario) {
        return switch (scenario) {
            case "agreement" -> ConsensusPolicy.tally(List.of(
                    new Vote(RecommendedAction.ROLLBACK, 80, "Recent deploy"),
                    new Vote(RecommendedAction.ROLLBACK, 75, "Restore logins"),
                    new Vote(RecommendedAction.MONITOR, 90, "Avoid disruption")));
            case "disagreement" -> ConsensusPolicy.tally(List.of(
                    new Vote(RecommendedAction.ROLLBACK, 99, "Restore reliability"),
                    new Vote(RecommendedAction.SCALE_UP, 80, "Preserve transactions"),
                    new Vote(RecommendedAction.MONITOR, 70, "Insufficient evidence")));
            default -> throw new NotFoundException("Use agreement or disagreement");
        };
    }
}
