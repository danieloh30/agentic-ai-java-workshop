package com.incidentmanagement.policy;

import java.time.Instant;

public final class RecoveryPolicy {
    private RecoveryPolicy() {}

    // Ex18: uncertain model work is marked interrupted, never silently executed twice.
    public static boolean abandoned(String status, Instant updatedAt, Instant cutoff) {
        // TODO Ex18: Identify only PLANNING attempts older than the cutoff.
        return false;
    }
}
