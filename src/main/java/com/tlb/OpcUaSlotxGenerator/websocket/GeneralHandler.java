package com.tlb.OpcUaSlotxGenerator.websocket;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SlotGuiData;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.UaSlotBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Component
public class GeneralHandler implements WebSocketHandler {
    private Logger log = LoggerFactory.getLogger("GeneralHandler");
    private final SinksHolder sinksHolder;
    private final ObjectMapper mapper;
    private final Map<String, PhsWebsocketMessage<?>> messagesCache = new HashMap<>();
    private final OpcUaSlotsProvider slotsProvider;
    private final InboundMessageHandler handler;

    @Autowired
    public GeneralHandler(SinksHolder sinksHolder, OpcUaSlotsProvider slotsProvider, InboundMessageHandler handler) {
        super();
        this.sinksHolder = sinksHolder;
        this.slotsProvider = slotsProvider;
        this.handler = handler;
        this.mapper = new ObjectMapper();
        log.info("Handler sink holder - {}", sinksHolder);
    }

    public void sendToClient(int id, PhsWebsocketMessage<?> message) {
        String json = serializeToJson(message);
        WebsocketHoldingEntity holdingEntity = sinksHolder.getById(id);
        if(holdingEntity != null)
            holdingEntity.getSink().tryEmitNext(json);
        else
            throw new NoSuchElementException("No element with id: " + id);
    }
    public void sendToClients(PhsWebsocketMessage<?> message) {
        String json = serializeToJson(message);
        //Adding message to cache, so it will be sent to new connected clients too
        sinksHolder.cacheMessage(message.type, message);
        for(WebsocketHoldingEntity holdingEntity : sinksHolder.getAll()) {
            try {
                //Taking sink specified for each client and sanding message to client
                holdingEntity.getSink().tryEmitNext(json);
            } catch (Exception e) {
                log.info("Exception while sending message to - {}", holdingEntity.getSession().getHandshakeInfo().getRemoteAddress());
            }

        }
    }
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<String> fluxOutput = sink.asFlux();
        Mono<Void> input = session.receive().map(WebSocketMessage::getPayloadAsText).doOnNext(msg -> {
            log.info("new msg = {}", msg);
        }).filter(msg -> {
            if(msg.equals("PING")) {
                sink.tryEmitNext("PONG");
                return false;
            } else {
                return true;
            }
        }).map(msg -> {
            PhsWebsocketMessage<?> message = null;
            try {
                message = mapper.readValue(msg, PhsWebsocketMessage.class);
            } catch (JsonProcessingException e) {
                log.info("exception while parsing to PHS websocketMessage");
            }
            return message;
        }).doOnNext(handler::addToQueue).then();
        try {
            sinksHolder.addConnection(new WebsocketHoldingEntity(sink, session));
        } catch (Exception e) {
            log.info("!!!!!!!!!!!!! - ", e);
        }



        log.info("info 1 - {}", session.getAttributes().values());
        log.info("info 2 - {}", session.getHandshakeInfo().getUri());
        log.info("info 3 - {}", session.getHandshakeInfo().getLogPrefix());
        sink.tryEmitNext(session.getId());
        while (!slotsProvider.isAfterInit()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        List<SlotGuiData> slots = slotsProvider.getSlotToAdd().values().stream().map(UaSlotBase::getSlotGuiData).toList();
        sink.tryEmitNext(serializeToJson(new PhsWebsocketMessage<>("SLOTS-ARRAY", slots)));

        try {
            for (PhsWebsocketMessage<?> msg : sinksHolder.getAllCachedMessage()) {
                sink.tryEmitNext(serializeToJson(msg));
                log.info("send serialized message: {} to {}", serializeToJson(msg), session.getHandshakeInfo().getRemoteAddress());
            }
        } catch (Exception e) {
            log.info("exception sending cached massages = ", e);
        }
        Mono<Void> output = session.send(fluxOutput.map(session::textMessage));
        return Mono.zip(input, output).then();
    }
    private String serializeToJson(PhsWebsocketMessage<?> message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
