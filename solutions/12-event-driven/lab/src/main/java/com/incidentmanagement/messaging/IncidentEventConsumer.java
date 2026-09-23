package com.incidentmanagement.messaging;

import com.incidentmanagement.agentic.workflow.IncidentResolutionWorkflow;
import com.incidentmanagement.model.IncidentEvent;
import com.incidentmanagement.model.ResolutionEvent;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The heart of the event-driven pattern: an agentic pipeline wired between two Kafka topics.
 * (Exercise 12, Step 2)
 *
 * <p>Your job is to turn {@code process(...)} into a Kafka processor: consume each incident from
 * {@code incidents-in}, run the agentic {@link IncidentResolutionWorkflow}, and publish the result
 * to {@code resolutions-out}. The {@link ResolutionSink} on the other end persists it.
 *
 * <p>TODO (Step 2):
 *   1. Annotate {@code process(...)} with:
 *        {@code @Incoming("incidents-in")}
 *        {@code @Outgoing("resolutions-out")}
 *        {@code @Blocking}   // the LLM calls block, so run on a worker thread
 *   2. Replace the body: call {@code workflow.process(event)} and return the ResolutionEvent.
 *
 * Imports you'll need:
 *   import org.eclipse.microprofile.reactive.messaging.Incoming;
 *   import org.eclipse.microprofile.reactive.messaging.Outgoing;
 *   import io.smallrye.reactive.messaging.annotations.Blocking;
 */
@ApplicationScoped
public class IncidentEventConsumer {

    @Inject
    IncidentResolutionWorkflow workflow;

    // TODO Step 2: add @Incoming("incidents-in") @Outgoing("resolutions-out") @Blocking,
    // then call the workflow and return its ResolutionEvent.
    public ResolutionEvent process(IncidentEvent event) {
        Log.infof("Consuming incident event #%d", event.incidentId());
        throw new UnsupportedOperationException("TODO: run the pipeline and return the ResolutionEvent (Exercise 12)");
    }
}
