package com.incidentmanagement.model;

import java.util.List;

public record Recommendation(
        String action,
        String rationale,
        List<String> evidence,
        List<String> verificationChecks,
        boolean requiresApproval) {}
