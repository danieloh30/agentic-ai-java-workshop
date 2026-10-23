package com.incidentmanagement.messaging;

import com.incidentmanagement.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.*;
import java.util.concurrent.CompletionStage;

/** Register in a short transaction, then publish; publication failure stays visible. */
@ApplicationScoped
public class IncidentPublisher {
    @Inject ResolutionStore store;
    @Inject @Channel("incidents-out") Emitter<IncidentEvent> emitter;

    public IncidentEvent publish(Integer incidentId, String report) {
        IncidentEvent event = store.register(incidentId, report);
        send(event);
        return event;
    }
    public IncidentEvent replay(String requestId) {
        IncidentEvent event = store.input(requestId);
        send(event);
        return event;
    }
    private void send(IncidentEvent event) {
        try {
            CompletionStage<Void> published = emitter.send(event);
            published.whenComplete((ignored, failure) -> {
                if (failure != null) failed(event);
            });
        } catch (RuntimeException failure) { failed(event); throw failure; }
    }
    private void failed(IncidentEvent event) {
        store.record(new ResolutionEvent(event.requestId(), event.incidentId(), "FAILED", "",
                "Publication failed; operator review required"));
    }
}
