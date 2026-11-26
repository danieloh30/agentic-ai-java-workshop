package com.incidentmanagement.impact;

import com.google.protobuf.util.JsonFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoJsonUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageRequest;
import org.a2aproject.sdk.spec.*;

/** Supplied, synchronous A2A HTTP adapter. All reasoning stays in ImpactAgent. */
public final class ImpactProtocol {
    private ImpactProtocol() {}

    public static String card(String publicUrl) throws Exception {
        var card = AgentCard.builder().name("impact-agent")
                .description("Estimates business impact and SLA cost for incident escalation decisions")
                .url(publicUrl).version("1.0.0")
                .capabilities(AgentCapabilities.builder().streaming(false).pushNotifications(false).build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("impact-assessment").name("Business impact assessment")
                        .description("Estimates business impact and SLA cost for incident escalation decisions")
                        .tags(List.of("impact-assessment", "sla-analysis")).build()))
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), publicUrl)))
                .build();
        return ProtoJsonUtils.toJson(JsonFormat.printer(), ProtoUtils.ToProto.agentCard(card));
    }

    public static String handle(String body, Function<String[], String> assessment) {
        Object id = null;
        try {
            var request = JSONRPCUtils.parseRequestBody(body, null);
            id = request.getId();
            if (!(request instanceof SendMessageRequest send))
                return JSONRPCUtils.toJsonRPCErrorResponse(id, new UnsupportedOperationError());
            String input = send.getParams().message().parts().stream()
                    .filter(TextPart.class::isInstance).map(TextPart.class::cast).map(TextPart::text)
                    .collect(Collectors.joining("\n"));
            String[] values = input.split("\\R", 4);
            if (input.length() > 8000 || values.length != 4
                    || java.util.Arrays.stream(values).anyMatch(String::isBlank)
                    || !values[2].matches("[1-4]"))
                return JSONRPCUtils.toJsonRPCErrorResponse(id, new InvalidParamsError());
            var message = Message.builder().role(Message.Role.ROLE_AGENT)
                    .messageId(UUID.randomUUID().toString())
                    .parts(new TextPart(assessment.apply(values))).build();
            return JSONRPCUtils.toJsonRPCResultResponse(id, ProtoUtils.ToProto.taskOrMessage(message));
        } catch (org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException
                | IllegalArgumentException invalid) {
            return JSONRPCUtils.toJsonRPCErrorResponse(id, new InvalidParamsError());
        } catch (Exception failed) {
            // Do not expose model/provider failures, prompts, or credentials on the wire.
            return JSONRPCUtils.toJsonRPCErrorResponse(id, new org.a2aproject.sdk.spec.InternalError("Impact assessment failed"));
        }
    }
}
