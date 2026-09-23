package com.incidentmanagement.messaging;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.incidentmanagement.model.ResolutionEvent;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A tiny in-memory index of the latest resolution per incident, so the REST layer can show
 * what the pipeline produced. In production this would be a database or a materialized view
 * over the {@code resolutions-out} topic; here it keeps the exercise focused on the flow.
 */
@ApplicationScoped
public class ResolutionStore {

    private final ConcurrentMap<Integer, ResolutionEvent> byIncident = new ConcurrentHashMap<>();

    public void record(ResolutionEvent event) {
        if (event != null && event.incidentId() != null) {
            byIncident.put(event.incidentId(), event);
        }
    }

    public ResolutionEvent get(Integer incidentId) {
        return byIncident.get(incidentId);
    }

    public Collection<ResolutionEvent> all() {
        return byIncident.values();
    }
}
