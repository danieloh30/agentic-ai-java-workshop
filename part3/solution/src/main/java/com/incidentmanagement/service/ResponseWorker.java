package com.incidentmanagement.service;


import io.quarkus.scheduler.Scheduled;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;

@ApplicationScoped
public class ResponseWorker {
    @Inject ResponseStore store;
    @Inject ModelInvocations agent;

    @ConfigProperty(name = "workshop.planning-lease-seconds", defaultValue = "120")
    long lease;

    @Scheduled(every = "2s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void tick() {
        store.recover(Instant.now().minusSeconds(lease));
        processNext(); // Ex17: short claim, model work outside a transaction, persisted proposal.
    }

    public void processNext() {
        var request = store.claim();
        if (request == null) return;
        try {
            store.complete(
                    request.id,
                    request.attempts,
                    agent.recommend(request.priority, request.report));
        } catch (Exception e) {
            store.fail(request.id, request.attempts);
        }
    }
}
