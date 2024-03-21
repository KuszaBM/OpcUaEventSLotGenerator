package com.tlb.OpcUaSlotxGenerator.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class PhsHandler {
    Logger log = LoggerFactory.getLogger(PhsHandler.class);
    private final WebClient webClient;
    private final Map<Integer, Emitter<Object>> emitterMap;

    @Autowired
    public PhsHandler(WebClient webClient) {
        this.webClient = webClient;
        this.emitterMap = new HashMap<>();
    }

    public void handleInput(int slotId, Object req) {
        emitterMap.get(slotId).emit(req);
    }
    public void handleOutput(int slotId, Object req) {
        try {
                Mono<String> resp = webClient.post()
                        .uri("http://127.0.0.1:8084/callFromPlc/" + slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(req))
                        .retrieve()
                        .bodyToMono(String.class);
                resp.subscribe();
            } catch (Exception e) {
                log.info("bad - ", e);
            }
    }
    public void newSlot(int slotId) {
        emitterMap.put(slotId, new Emitter<Object>());
    }

    public Map<Integer, Emitter<Object>> getEmitterMap() {
        return emitterMap;
    }
}
