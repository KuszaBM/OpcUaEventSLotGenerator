package com.tlb.OpcUaSlotxGenerator.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlb.OpcUaSlotxGenerator.PhsWebsocketMessage;

public class PhsMessageJsonizer {
    public static String serializeToJson(PhsWebsocketMessage<?> message) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
