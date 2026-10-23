package com.incidentmanagement.messaging;

/** Exercise 14: reuse a terminal outcome before running the agents again. */
public final class ReplayPolicy {
    private ReplayPolicy() {}
    public static boolean reuseCompleted() {
        // TODO Ex14: reuse completed outcomes without running the model again.
        return false;
    }
}
