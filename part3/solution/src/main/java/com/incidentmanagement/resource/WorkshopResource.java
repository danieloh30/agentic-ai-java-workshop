package com.incidentmanagement.resource;

import com.incidentmanagement.model.*;
import com.incidentmanagement.policy.RecommendationChecks;
import com.incidentmanagement.service.*;
import com.incidentmanagement.service.ModelInvocations;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.*;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkshopResource {
    public record Submission(String requestId, Long incidentId, String report) {}

    public record Preview(String priority, String report) {}

    public record Decision(String proposalHash, boolean approve) {}

    public record Label(String label) {}

    @Inject ModelInvocations agent;
    @Inject ResponseStore store;
    @Inject EvaluationService evaluations;
    @Inject EvaluationService.EvaluationRepository runs;

    @GET
    @Path("/incidents")
    @Transactional
    public List<IncidentInfo> incidents() {
        return IncidentInfo.listAll();
    }

    @POST
    @Path("/preview")
    public Recommendation preview(Preview p) {
        if (p == null
                || p.report() == null
                || p.report().isBlank()
                || p.report().length() > 8000
                || !List.of("P1", "P2", "P3", "P4")
                        .contains(p.priority() == null ? "" : p.priority()))
            throw new WebApplicationException(400);
        var r = agent.recommend(p.priority(), p.report());
        if (!RecommendationChecks.grounded(r, p.report()))
            throw new WebApplicationException("Evidence is not quoted from the report", 422);
        return r;
    }

    @POST
    @Path("/responses")
    public Response submit(Submission s) {
        if (s == null || s.incidentId() == null) throw new WebApplicationException(400);
        var r = store.create(s.requestId(), s.incidentId(), s.report());
        return Response.accepted(r).location(java.net.URI.create("/api/responses/" + r.id)).build();
    }

    @GET
    @Path("/responses")
    public List<ResponseRequest> responses() {
        return store.all();
    }

    @GET
    @Path("/responses/{id}")
    public ResponseRequest response(@PathParam("id") String id) {
        return store.get(id);
    }

    @POST
    @Path("/responses/{id}/decision")
    public ResponseRequest decide(@PathParam("id") String id, Decision d) {
        if (d == null) throw new WebApplicationException(400);
        return store.decide(id, d.proposalHash(), d.approve());
    }

    @POST
    @Path("/responses/{id}/retry")
    public ResponseRequest retry(@PathParam("id") String id) {
        return store.retry(id);
    }

    @POST
    @Path("/evaluations")
    public EvaluationRun evaluate(Label label) throws Exception {
        if (label == null) throw new WebApplicationException(400);
        return evaluations.run(label.label());
    }

    @GET
    @Path("/evaluations")
    public List<EvaluationRun> evaluations() {
        return runs.all();
    }
}
