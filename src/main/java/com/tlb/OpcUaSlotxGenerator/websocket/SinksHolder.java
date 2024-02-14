package com.tlb.OpcUaSlotxGenerator.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SinksHolder {
    private static SinksHolder instance;
    private final Map<Integer, WebsocketHoldingEntity> map;
    private final Map<String, PhsWebsocketMessage<?>> messagesCache = new HashMap<>();
    private int id;
    private Logger log = LoggerFactory.getLogger("SinkHolder");

    public static SinksHolder getInstance() {
        if(instance == null) {
            instance = new SinksHolder();
        }
        return instance;
    }
    private SinksHolder() {
        this.id = 0;
        this.map = new HashMap<>();
    }
    public void sendToAll(PhsWebsocketMessage<?> message) {
        log.info("sending from - {}", this);
        for(WebsocketHoldingEntity e : getAll()) {
            e.getSink().tryEmitNext(PhsMessageJsonizer.serializeToJson(message));
        }
    }
    public void cacheMessage(String messageType, PhsWebsocketMessage<?> message) {
        try {
            messagesCache.put(messageType, message);
        } catch (Exception e) {
            log.info("exc - ", e);
        }

    }
    public List<PhsWebsocketMessage<?>> getAllCachedMessage() {
        return new ArrayList<>(messagesCache.values());
    }
    public void addConnection(WebsocketHoldingEntity entity) {
        id++;
        for(WebsocketHoldingEntity e : map.values()) {
            if(Objects.equals(e.getSession().getHandshakeInfo().getRemoteAddress(), entity.getSession().getHandshakeInfo().getRemoteAddress()))
                return;
        }
        map.put(id, entity);
        log.info("new client added | id: {} - session: {}", id, entity.getSession().getHandshakeInfo().getRemoteAddress());
    }
    public void deleteConnection(int id) {
        map.remove(id);
    }
    public WebsocketHoldingEntity getById(int id) {
        return map.get(id);
    }
    public List<WebsocketHoldingEntity> getAll() {
        return new ArrayList<>(map.values());
    }

}
