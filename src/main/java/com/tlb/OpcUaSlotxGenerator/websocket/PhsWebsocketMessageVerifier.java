package com.tlb.OpcUaSlotxGenerator.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlb.OpcUaSlotxGenerator.PhsWebsocketMessage;
import org.slf4j.LoggerFactory;

public class PhsWebsocketMessageVerifier {

    /**
     *
     * @param messageJson - String input
     * @return - bool value depend on parsing messageJson to {@link  PhsWebsocketMessage}
     */
    public static boolean tryToParseMessage(String messageJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue(messageJson, PhsWebsocketMessage.class);
            return true;
        } catch (JsonProcessingException e) {
            LoggerFactory
                    .getLogger(PhsWebsocketMessageVerifier.class)
                    .info("exception while parsing to PHS websocketMessage ");
        }
        return false;
    }
}
