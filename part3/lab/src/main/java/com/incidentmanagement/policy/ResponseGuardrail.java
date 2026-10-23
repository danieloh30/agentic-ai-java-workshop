package com.incidentmanagement.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentmanagement.model.Recommendation;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResponseGuardrail implements OutputGuardrail {
    @Inject ObjectMapper mapper;

    @Override
    public OutputGuardrailResult validate(AiMessage message) {
        try {
            var errors =
                    RecommendationChecks.violations(
                            mapper.readValue(message.text(), Recommendation.class));
            if (errors.isEmpty()) return success();
            // TODO Ex16: Return reprompt(...) with correction feedback instead of immediate
            // rejection.
            return fatal(String.join("; ", errors));
        } catch (Exception invalid) {
            // TODO Ex16: Request a valid JSON repair, within the agent's existing retry budget.
            return fatal("Invalid recommendation JSON");
        }
    }
}
