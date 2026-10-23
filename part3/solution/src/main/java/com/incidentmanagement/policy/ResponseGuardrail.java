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
            String feedback = String.join("; ", errors);
            return reprompt(feedback, feedback);
        } catch (Exception invalid) {
            return reprompt("Invalid recommendation JSON", "Invalid recommendation JSON");
        }
    }
}
