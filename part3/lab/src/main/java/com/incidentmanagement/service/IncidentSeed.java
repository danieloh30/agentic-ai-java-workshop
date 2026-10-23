package com.incidentmanagement.service;

import com.incidentmanagement.model.*;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

/** Seed once; never erase participant work on a dev-mode reload. */
@ApplicationScoped
public class IncidentSeed {
    @Transactional
    void seed(@Observes StartupEvent startup) {
        if (IncidentInfo.count() != 0) return;
        String[][] rows = {
            {
                "payment-gateway",
                "checkout-api",
                "P2",
                "Intermittent 503 errors during peak hours",
                "OPEN"
            },
            {"auth-service", "user-login", "P1", "Complete authentication failure", "IN_PROGRESS"},
            {"inventory-db", "stock-sync", "P3", "Stale inventory data, sync lag 15 min", "OPEN"},
            {"cdn-edge", "static-assets", "P4", "Slow asset loading in EU region", "TRIAGING"},
            {
                "email-service",
                "notification-api",
                "P2",
                "Delivery failures for order confirmations",
                "OPEN"
            },
            {
                "search-engine",
                "product-search",
                "P3",
                "Relevance degradation after index rebuild",
                "OPEN"
            },
            {"monitoring", "alerting-api", "P2", "Alert fatigue — 40% false positive rate", "OPEN"},
            {
                "api-gateway",
                "rate-limiter",
                "P1",
                "Rate limiter rejecting legitimate traffic",
                "IN_PROGRESS"
            }
        };
        for (String[] row : rows) {
            var incident = new IncidentInfo();
            incident.system = row[0];
            incident.service = row[1];
            incident.priority = row[2];
            incident.description = row[3];
            incident.status = IncidentStatus.valueOf(row[4]);
            incident.persist();
        }
    }
}
