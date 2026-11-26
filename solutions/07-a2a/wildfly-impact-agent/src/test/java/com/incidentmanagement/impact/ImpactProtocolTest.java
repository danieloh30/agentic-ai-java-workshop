package com.incidentmanagement.impact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageResponse;
import org.a2aproject.sdk.spec.*;
import org.junit.jupiter.api.Test;

class ImpactProtocolTest {
    static String request(String input) {
        var message = Message.builder().role(Message.Role.ROLE_USER)
                .messageId(UUID.randomUUID().toString()).parts(new TextPart(input)).build();
        return JSONRPCUtils.toJsonRPCRequest("impact-1", A2AMethods.SEND_MESSAGE_METHOD,
                ProtoUtils.ToProto.sendMessageRequest(MessageSendParams.builder().message(message).build()));
    }

    @Test
    void sdkRoundTripPreservesMultilineDescriptionAndRequestId() throws Exception {
        String json = ImpactProtocol.handle(request("payment-gateway\ncheckout-api\n1\n503 outage\nall endpoints fail"), values -> {
            assertArrayEquals(new String[]{"payment-gateway", "checkout-api", "1", "503 outage\nall endpoints fail"}, values);
            return "Estimated Impact: $36,000\nJustification: Revenue-critical outage; duration assumed.";
        });
        var response = (SendMessageResponse) JSONRPCUtils.parseResponseBody(json, A2AMethods.SEND_MESSAGE_METHOD);
        assertEquals("impact-1", response.getId());
        assertTrue(response.getResult() instanceof Message);
        var result = (Message) response.getResult();
        assertEquals(Message.Role.ROLE_AGENT, result.role());
        assertTrue(((TextPart) result.parts().getFirst()).text().contains("$36,000"));
    }

    @Test
    void invalidInputsNeverInvokeTheModel() {
        for (String input : new String[]{"checkout-api\nP1", "checkout-api\n\nP1\noutage", "x".repeat(8001)}) {
            String result = ImpactProtocol.handle(request(input), values -> { fail("Invalid input reached model"); return ""; });
            assertTrue(result.contains("\"error\""));
        }
    }

    @Test
    void providerFailureDoesNotLeakItsMessage() {
        String result = ImpactProtocol.handle(request("payment-gateway\ncheckout-api\n1\noutage"), values -> {
            throw new IllegalStateException("secret-provider-detail");
        });
        assertTrue(result.contains("\"error\""));
        assertFalse(result.contains("secret-provider-detail"));
    }

    @Test
    void cardAdvertisesTheContainerReachableUrlAndBlockingCapability() throws Exception {
        String card = ImpactProtocol.card("http://localhost:8888/");
        assertTrue(card.contains("http://localhost:8888/"));
        assertTrue(card.contains("impact-assessment"));
        assertFalse(card.contains("\"streaming\": true"));
    }
}
