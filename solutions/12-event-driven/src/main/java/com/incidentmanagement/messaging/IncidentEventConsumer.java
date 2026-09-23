package com.incidentmanagement.messaging;

import com.incidentmanagement.agentic.workflow.IncidentResolutionWorkflow;
import com.incidentmanagement.model.IncidentEvent;
import com.incidentmanagement.model.ResolutionEvent;

import io.smallrye.reactive.messaging.annotations.Blocking;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

/**
 * The heart of the event-driven pattern: an agentic pipeline wired between two Kafka topics.
 *
 * <p>Each message on {@code incidents-in} is handed to the {@link IncidentResolutionWorkflow}
 * (triage → resolve), and the resulting {@link ResolutionEvent} is published to
 * {@code resolutions-out}. Because the agent calls block on the LLM, the method is
 * {@code @Blocking} — SmallRye runs it on a worker thread so the event loop stays free.
 */
@ApplicationScoped
public class IncidentEventConsumer {

    @Inject
    IncidentResolutionWorkflow workflow;

    @Incoming("incidents-in")
    @Outgoing("resolutions-out")
    @Blocking
    public ResolutionEvent process(IncidentEvent event) {
        Log.infof("Consuming incident event #%d (%s/%s P%s)",
                event.incidentId(), event.system(), event.service(), event.priority());

        ResolutionEvent result = workflow.process(event);

        Log.infof("Pipeline resolved incident #%d → publishing to resolutions-out", event.incidentId());
        return result;
    }
}
