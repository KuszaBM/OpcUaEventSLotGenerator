package com.tlb.OpcUaSlotxGenerator.opcUa.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaClientProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SLotGuiPropagator;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SlotGuiData;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UaSlotBase {
    public SlotGuiData getSlotGuiData() {
        return slotGuiData;
    }

    private int slotId = -1;
    private List<String> requests;
    private NodeId tokenId;
    private SlotType slotType;
    private OpcUaClientProvider opcUaClientProvider;
    private SlotGuiData slotGuiData;
    private AtomicBoolean inAckMode = new AtomicBoolean(false);
    private int slotNo;
    private String slotName;
    private String opcUaName;
    private int namespace;
    protected Logger logger = LoggerFactory.getLogger(UaSlotBase.class);
    public UaSlotBase(int slotId, SlotType type, int nameSpace, String opcUaName) {
        this.slotId = slotId;
        this.slotType = type;
        this.opcUaName = opcUaName;
        this.namespace = nameSpace;
        this.slotName = "SLOT_" + slotId;
        String s = "\"" + opcUaName + "\".";
        String tokenNodeName = SlotType.ToPlc.equals(type) ? "IN" : "OUT";
        String s2 = "\"" + slotName + "_TOKEN_" + tokenNodeName + "\"";
        this.tokenId = new NodeId(nameSpace, s + s2);
        logger.info("New slot created, slot name - {} | nodeId = {}", slotName, tokenId);
    }
    public UaSlotBase(int slotId, SlotType type, NodeId tokenNodeId, int nameSpace, String opcUaName, OpcUaClientProvider clientProvider, SLotGuiPropagator propagator) {
        this.opcUaClientProvider = clientProvider;
        this.slotId = slotId;
        this.slotType = type;
        this.tokenId = tokenNodeId;
        this.opcUaName = opcUaName;
        this.namespace = nameSpace;
        this.slotName = "SLOT_" + slotId;
        this.slotGuiData = new SlotGuiData(slotId, SlotType.ToPlc.equals(type) ? "IN" : "OUT", propagator);
    }

    public void checkResponseState() {
        int count = 0;
        boolean tokenAckValue = getSlotType().equals(SlotType.ToPlc);
        try {
            boolean opcTokenState = (Boolean) getOpcUaClientProvider().getClient().getAddressSpace().getVariableNode(tokenId).readValue().getValue().getValue();
            while (opcTokenState == tokenAckValue) {
                if(count == 100)
                    count = 0;
                if(count == 0) {
                    logger.info("SLOT {} - OPC token state not ready for read - {}",slotId, opcTokenState);
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                opcTokenState = (Boolean) getOpcUaClientProvider().getClient().getAddressSpace().getVariableNode(tokenId).readValue().getValue().getValue();
                count++;
            }

        } catch (UaException e) {
            logger.info("Exception reading OPC - ", e);
        }
    }

    public void writeSlotAck() {
        try {
            setInAckMode(true);
            boolean tokenAckValue = getSlotType().equals(SlotType.ToPlc);
            Variant writeValue = new Variant(tokenAckValue);
            DataValue dataValue = DataValue.valueOnly(writeValue);
            logger.info("WRITING TOKEN {}",tokenId);
            CompletableFuture<StatusCode> statusCompletable = getOpcUaClientProvider().getClient().writeValue(tokenId, dataValue);
            StatusCode statusCode = statusCompletable.get();
            logger.info("SLOT {} - Token ack to opc value - {} | Response - {}",slotId, writeValue, statusCode);
            boolean opcTokenState = (Boolean) getOpcUaClientProvider().getClient().getAddressSpace().getVariableNode(tokenId).readValue().getValue().getValue();
            logger.info("SLOT {} - Token state after write - {}", slotId, opcTokenState);
            if(opcTokenState != tokenAckValue) {
                logger.info("Writing token not done ");
//                while (opcTokenState != tokenAckValue) {
//                    if(!inAckMode.get()) {
//                        logger.info("token change received by PLC - BREAK");
//                        break;
//                    }
//                    CompletableFuture<StatusCode> rewriteStatusCompletable = getOpcUaClientProvider().getClient().writeValue(tokenId, dataValue);
//                    StatusCode rewriteStatus = rewriteStatusCompletable.get();
//                    logger.info("SLOT {} - Renew Token ack to opc value - {} | Response - {}",slotId, writeValue, rewriteStatus);
//                    opcTokenState = (Boolean) getOpcUaClientProvider().getClient().getAddressSpace().getVariableNode(tokenId).readValue().getValue().getValue();
//                    logger.info("SLOT {} - Renew Token state after write - {}", slotId, opcTokenState);
//                }
            }
            setInAckMode(false);
            logger.info("SLOT {} - finished ", slotId);

        } catch (InterruptedException | ExecutionException | UaException e) {
            throw new RuntimeException(e);
        }
    }


    //Getters & Setters

    public void setInAckMode(boolean tokenState) {
        logger.info("Setting inAckMode for SLOT {} | {} --> {}", slotId, inAckMode.get(), tokenState);
        this.inAckMode.set(tokenState);
    }
    public int getSlotNo() {
        return slotNo;
    }

    public int getSlotId() {
        return slotId;
    }

    public String getSlotName() {
        return slotName;
    }

    public String getOpcUaName() {
        return opcUaName;
    }

    public int getNamespace() {
        return namespace;
    }

    public NodeId getTokenId() {
        return tokenId;
    }
    public void setTokenId(NodeId tokenId) {
        this.tokenId = tokenId;
    }
    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }
    public SlotType getSlotType() {
        return slotType;
    }
    public OpcUaClientProvider getOpcUaClientProvider() {
        return opcUaClientProvider;
    }

    public AtomicBoolean getInAckMode() {
        return inAckMode;
    }
}
