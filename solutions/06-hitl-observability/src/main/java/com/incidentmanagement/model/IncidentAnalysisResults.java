package com.incidentmanagement.model;

/**
 * Record containing the three incident analysis results.
 * These are the outputs from the parallel incident analysis workflow.
 */
public record IncidentAnalysisResults(
        String severityAnalysis,
        String impactAnalysis,
        String resolutionAnalysis
) {
}
