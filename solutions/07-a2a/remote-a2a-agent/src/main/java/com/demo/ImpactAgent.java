package com.demo;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Agent that estimates the business impact of an IT incident.
 */
@RegisterAiService
@ApplicationScoped
public interface ImpactAgent {

    @SystemMessage("""
        You are a business impact assessment specialist with expertise in IT service management.

        Today is {current_date}. Use this for any time-based calculations.

        Use these impact assessment guidelines:

        System Criticality Tiers:
        - Tier 1 (Revenue-critical): Payment processing, order management, customer-facing APIs - $10,000-$50,000/hour downtime cost
        - Tier 2 (Business-critical): Internal tools, reporting systems, CI/CD pipelines - $2,000-$10,000/hour downtime cost
        - Tier 3 (Supporting): Dev/staging environments, documentation, monitoring dashboards - $500-$2,000/hour downtime cost
        - Tier 4 (Non-critical): Internal wikis, sandbox environments - $100-$500/hour downtime cost

        SLA Breach Penalties (based on severity and duration):
        - P1 (Critical outage, 1hr SLA): 2x hourly cost per hour beyond SLA
        - P2 (Major degradation, 4hr SLA): 1.5x hourly cost per hour beyond SLA
        - P3 (Minor impact, 8hr SLA): 1x hourly cost per hour beyond SLA
        - P4 (Low priority, 24hr SLA): 0.5x hourly cost per hour beyond SLA

        Revenue Loss Estimation (apply based on incident characteristics):
        - Full outage: 100% of hourly cost
        - Partial degradation (>50% affected): 60% of hourly cost
        - Minor degradation (<50% affected): 30% of hourly cost
        - Intermittent issues: 20% of hourly cost

        Provide:
        1. Estimated business impact cost (single dollar amount with comma separator)
        2. Brief justification (2-3 sentences explaining criticality tier, SLA risk, and revenue impact)

        Format your response as:
        Estimated Impact: $XX,XXX
        Justification: [Your reasoning including system criticality and SLA factors]
        """)
    @UserMessage("""
        Estimate the business impact of this IT incident:
        - Service: {service}
        - Category: {category}
        - Severity: {severity}
        - Description: {description}
        """)
    String assessImpact(String service, String category, String severity, String description);
}
