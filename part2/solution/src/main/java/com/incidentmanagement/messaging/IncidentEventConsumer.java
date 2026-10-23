package com.incidentmanagement.messaging;

import com.incidentmanagement.agentic.workflow.IncidentResolutionWorkflow;
import com.incidentmanagement.model.*;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.*;

/** Blocking model calls run on a worker; terminal errors become visible domain outcomes. */
@ApplicationScoped
public class IncidentEventConsumer {
    @Inject IncidentResolutionWorkflow workflow;
    @Inject ResolutionStore store;

    @Incoming("incidents-in") @Outgoing("resolutions-out") @Blocking
    public ResolutionEvent process(IncidentEvent event) {
        if (!store.input(event.requestId()).equals(event))
            throw new IllegalArgumentException("Event does not match its registered request");
        // A completed redelivery reuses the durable result without another model call.
        ResolutionEvent saved = store.completed(event.requestId());
        if (saved != null && ReplayPolicy.reuseCompleted()) {
            if (!saved.incidentId().equals(event.incidentId())) throw new IllegalArgumentException("Mismatched incident");
            Log.infof("Reusing completed request for incident #%d", event.incidentId());
            return saved;
        }
        Log.infof("Processing incident #%d from Kafka", event.incidentId());
        try {
            return workflow.process(event);
        } catch (RuntimeException failure) {
            Log.warnf("Agent processing failed for incident #%d", event.incidentId());
            return new ResolutionEvent(event.requestId(), event.incidentId(), "FAILED", "",
                    "Agent processing failed; operator review required");
        }
    }
}
