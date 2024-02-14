package com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui;

import com.tlb.OpcUaSlotxGenerator.websocket.PhsWebsocketMessage;
import com.tlb.OpcUaSlotxGenerator.websocket.SinksHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

public class SLotGuiPropagator {
    private final WebClient webClient;
    Logger log = LoggerFactory.getLogger(SLotGuiPropagator.class);
    private final SinksHolder sinksHolder;

    public SLotGuiPropagator(WebClient webClient, SinksHolder sinksHolder) {
        this.webClient = webClient;
        this.sinksHolder = sinksHolder;
    }
    public void propagateSlotChange(SlotGuiData slotGuiData) {
        PhsWebsocketMessage<?> message = new PhsWebsocketMessage<>("SLOT-UPDATE", slotGuiData);
        sinksHolder.sendToAll(message);
        log.info("propagate update {}", slotGuiData);
//        if(message.type.equals("SLOT-UPDATE")) {
//            try {
//                Mono<String> resp = webClient.post()
//                        .uri("http://127.0.0.1:8080/slots/update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .body(BodyInserters.fromValue(message))
//                        .retrieve()
//                        .bodyToMono(String.class);
//                resp.subscribe();
//            } catch (Exception e) {
//                log.info("bad - ", e);
//            }
//        }
    }

    public void propagateALlSlots(List<SlotGuiData> slotGuiData) {
        PhsWebsocketMessage<?> message = new PhsWebsocketMessage<>("SLOT-ARRAY", slotGuiData);
        if(message.type.equals("SLOT-ARRAY")) {
            try {
                Mono<String> resp = webClient.post()
                        .uri("http://127.0.0.1:8080/slots/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(message))
                        .retrieve()
                        .bodyToMono(String.class);
                resp.subscribe();
            } catch (Exception e) {
                log.info("bad - ", e);
            }
        }
    }

}
