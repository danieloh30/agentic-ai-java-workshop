package com.incidentmanagement.messaging;

import com.incidentmanagement.model.IncidentInfo;
import com.incidentmanagement.model.IncidentStatus;
import com.incidentmanagement.model.ResolutionEvent;

import io.smallrye.reactive.messaging.annotations.Blocking;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * The downstream consumer of {@code resolutions-out}: it records the resolution for the REST
 * layer to read and flips the incident to {@code RESOLVED} in the database.
 *
 * <p>Splitting this from {@link IncidentEventConsumer} is the point of an event-driven design:
 * the resolver publishes a fact ("this incident was resolved") and any number of independent
 * consumers can react to it — persist it, notify, audit — without the resolver knowing they exist.
 */
@ApplicationScoped
public class ResolutionSink {

    @Inject
    ResolutionStore store;

    @Incoming("resolutions-in")
    @Blocking
    @Transactional
    public void consume(ResolutionEvent event) {
        store.record(event);

        IncidentInfo incident = IncidentInfo.findById(event.incidentId());
        if (incident != null) {
            incident.status = IncidentStatus.RESOLVED;
            incident.persist();
        }
        Log.infof("Recorded resolution for incident #%d and marked it RESOLVED", event.incidentId());
    }
}
