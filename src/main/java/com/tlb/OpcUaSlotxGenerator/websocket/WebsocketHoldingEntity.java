package com.tlb.OpcUaSlotxGenerator.websocket;

import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

public class WebsocketHoldingEntity {
    private Sinks.Many<String> sink;
    private WebSocketSession session;

    public WebsocketHoldingEntity(Sinks.Many<String> sink, WebSocketSession session) {
        this.sink = sink;
        this.session = session;
    }

    public Sinks.Many<String> getSink() {
        return sink;
    }

    public WebSocketSession getSession() {
        return session;
    }
}
