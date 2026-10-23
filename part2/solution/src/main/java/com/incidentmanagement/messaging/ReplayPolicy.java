package com.incidentmanagement.messaging;

/** Exercise 14: reuse a terminal outcome before running the agents again. */
public final class ReplayPolicy {
    private ReplayPolicy() {}
    public static boolean reuseCompleted() {
        return true;
    }
}
