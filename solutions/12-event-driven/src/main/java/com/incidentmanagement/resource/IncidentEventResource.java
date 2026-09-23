package com.incidentmanagement.resource;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.incidentmanagement.messaging.ResolutionStore;
import com.incidentmanagement.model.IncidentEvent;
import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.ResolutionEvent;

import io.quarkus.logging.Log;

/**
 * The edge of the event-driven system: publish an incident onto Kafka, and read back what the
 * pipeline produced. Publishing returns immediately — the agentic work happens asynchronously
 * on the consumer — so you poll {@code GET /incident-events/resolutions/{id}} for the result.
 *
 * <p>The optional operator report is the request body (text/plain), e.g.
 * {@code curl -X POST .../incident-events/publish/2 -H 'Content-Type: text/plain' --data 'pods OOMKilled'}.
 */
@Path("/incident-events")
@Produces(MediaType.APPLICATION_JSON)
public class IncidentEventResource {

    @Channel("incidents-out")
    Emitter<IncidentEvent> emitter;

    @Inject
    ResolutionStore store;

    @POST
    @Path("/publish/{incidentId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional
    public Map<String, Object> publish(Integer incidentId, String report) {
        IncidentInfo incident = IncidentInfo.findById(incidentId);
        if (incident == null) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }

        IncidentEvent event = new IncidentEvent(incidentId, incident.system, incident.service,
                incident.priority, incident.description, report != null ? report : "");
        emitter.send(event);
        Log.infof("Published incident #%d to incidents-in", incidentId);

        var response = new LinkedHashMap<String, Object>();
        response.put("published", true);
        response.put("incidentId", incidentId);
        response.put("topic", "incidents-in");
        response.put("next", "GET /incident-events/resolutions/" + incidentId + " (once the pipeline finishes)");
        return response;
    }

    /** All resolutions produced so far (from the {@code resolutions-out} sink). */
    @GET
    @Path("/resolutions")
    public Collection<ResolutionEvent> resolutions() {
        return store.all();
    }

    /** The resolution for one incident, or 404 if the pipeline hasn't produced it yet. */
    @GET
    @Path("/resolutions/{incidentId}")
    public ResolutionEvent resolution(Integer incidentId) {
        ResolutionEvent event = store.get(incidentId);
        if (event == null) {
            throw new NotFoundException("No resolution yet for incident " + incidentId
                    + " — the pipeline may still be running.");
        }
        return event;
    }
}
