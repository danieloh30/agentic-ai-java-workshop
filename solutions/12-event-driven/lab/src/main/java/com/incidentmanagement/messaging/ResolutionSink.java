package com.incidentmanagement.messaging;

import com.incidentmanagement.model.ResolutionEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/** The transactional store commits before this method returns and messaging acknowledges. */
@ApplicationScoped
public class ResolutionSink {
    @Inject ResolutionStore store;
    @Incoming("resolutions-in") @Blocking
    public void consume(ResolutionEvent event) {
        store.record(event);
        Log.infof("Recorded processing outcome for incident #%d: %s", event.incidentId(), event.processingStatus());
    }
}
