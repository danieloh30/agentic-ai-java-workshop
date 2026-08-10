package com.demo;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;

@ApplicationScoped
public class ImpactAgentCard {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("impact-agent")
                .description("Estimates business impact and SLA cost for incident escalation decisions")
                .url("http://localhost:8888/")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                                .id("impact-assessment")
                                .name("Business impact assessment")
                                .description("Estimates business impact and SLA cost for incident escalation decisions")
                                .tags(List.of("impact-assessment", "sla-analysis"))
                                .build()))
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:8888/")))
                .build();
    }
}
