package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.UaSlotBase;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class OpcUaWriter <T> implements Consumer<T> {

    private UaSlotBase slotBase;
    Logger logger = LoggerFactory.getLogger(OpcUaWriter.class);
    NodeId tokenId;

//    public NodeId getWriteNodeId() {
//        String s = slotBase.getOpcUaName();
//        String s2 = a.name().isEmpty() ?
//                "\"" + f.getName() + "_" + slotBase.getSlotName() +  "\"" :
//                "\"" + a.name() + "_" + slotBase.getSlotName() +  "\"";
//        NodeId node = new NodeId(slotBase.getNamespace(), s + "." + s2);
//        return node;
//    }
    public OpcUaWriter(Class<T> cls, UaSlotBase slotBase) {
        this.slotBase = slotBase;
        this.tokenId = slotBase.getTokenId();
        for (Field f : cls.getDeclaredFields()) {
            OpcUaNode a = f.getAnnotation(OpcUaNode.class);
            if(a == null)
                continue;
            String s = slotBase.getOpcUaName();
            String s2 = a.name().isEmpty() ?
                    "\"" + f.getName() + "_" + slotBase.getSlotName() +  "\"" :
                    "\"" + a.name() + "_" + slotBase.getSlotName() +  "\"";
            NodeId node = new NodeId(slotBase.getNamespace(), s + "." + s2);

            logger.info("--- writing node - {} ", node);

            f.setAccessible(true);
            writes.add((t)->{
                try {;
                    Variant writeValue = new Variant(f.get(t));
                    DataValue dataValue = DataValue.valueOnly(writeValue);
                    logger.info("try to update node - {}", node);
                    CompletableFuture<StatusCode> status = slotBase.getOpcUaClientProvider().getClient().writeValue(node, dataValue);
                    logger.info("Send to Opc - {} | Response - {}", dataValue, status.get());
                } catch (IllegalAccessException | InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void waitIfNoConnection() {
        while (!slotBase.getOpcUaClientProvider().isConnected()) {
            logger.info("Slot {} blocked - waiting for connection", slotBase.getSlotId());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    @Override
    public void accept(T t) {
        waitIfNoConnection();
        try {
            for (Consumer<T> c : writes)
                c.accept(t);

            slotBase.writeSlotAck();

        } catch (Exception e) {
            logger.info("Exception while writing to PLC - Connection state: {}", slotBase.getOpcUaClientProvider().isConnected() ? "Connected" : "Not Connected");
            logger.info("MES - ", e);
            waitIfNoConnection();
            slotBase.writeSlotAck();
        }
        logger.info("Finished writers write to Opc");
    }
    private final List<Consumer<T>> writes = new ArrayList<>();
}

