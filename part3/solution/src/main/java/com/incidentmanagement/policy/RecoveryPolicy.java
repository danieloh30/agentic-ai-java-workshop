package com.incidentmanagement.policy;

import java.time.Instant;

public final class RecoveryPolicy {
    private RecoveryPolicy() {}

    // Ex18: uncertain model work is marked interrupted, never silently executed twice.
    public static boolean abandoned(String status, Instant updatedAt, Instant cutoff) {
        return "PLANNING".equals(status) && updatedAt.isBefore(cutoff);
    }
}
