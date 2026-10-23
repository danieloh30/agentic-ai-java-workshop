package com.incidentmanagement.resource;

import com.incidentmanagement.messaging.*;
import com.incidentmanagement.model.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;

@Path("/incident-events")
@Produces(MediaType.APPLICATION_JSON)
public class IncidentEventResource {
    @Inject IncidentPublisher publisher;
    @Inject ResolutionStore store;

    @POST @Path("/publish/{incidentId}") @Consumes(MediaType.TEXT_PLAIN)
    public Map<String, Object> publish(Integer incidentId, String report) {
        var event = publisher.publish(incidentId, report);
        return Map.of("submitted", true, "incidentId", incidentId, "requestId", event.requestId(),
                "next", "/incident-events/resolutions/" + incidentId);
    }
    @POST @Path("/replay/{requestId}")
    public Map<String, Object> replay(String requestId) {
        var event = publisher.replay(requestId);
        return Map.of("submitted", true, "requestId", event.requestId());
    }
    @GET @Path("/resolutions")
    public Collection<ResolutionEvent> resolutions() { return store.all(); }
    @GET @Path("/resolutions/{incidentId}")
    public ResolutionEvent resolution(Integer incidentId) {
        var result = store.get(incidentId);
        if (result == null) throw new NotFoundException("No processing request for this incident");
        return result;
    }
}
