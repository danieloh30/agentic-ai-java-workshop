package com.incidentmanagement.agentic.workflow;

import com.incidentmanagement.model.ExecutionPlan;
import com.incidentmanagement.model.ResolutionStep;
import java.util.HashSet;

/** Validate prerequisites before any specialist runs, including on replans. */
public final class PlanValidator {
    private PlanValidator() {}
    public static void validate(ExecutionPlan plan, boolean diagnosisExists, boolean mitigationExists) {
        if (plan == null || plan.steps() == null || plan.steps().isEmpty())
            throw new IllegalArgumentException("Plan must contain work");
        var seen = new HashSet<ResolutionStep>();
        for (var step : plan.steps()) {
            if (step == null || !seen.add(step)) throw new IllegalArgumentException("Invalid or duplicate step");
            switch (step) {
                case DIAGNOSE -> { diagnosisExists = true; mitigationExists = false; }
                case MITIGATE -> {
                    if (!diagnosisExists) throw new IllegalArgumentException("MITIGATE requires DIAGNOSE");
                    mitigationExists = true;
                }
                case VERIFY -> {
                    if (!mitigationExists) throw new IllegalArgumentException("VERIFY requires MITIGATE");
                }
                case COMMUNICATE -> { }
            }
        }
    }
}
