package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UaSlotBase {
    private int slotId = -1;
    private NodeId tokenId;
    private SlotType slotType;
    private OpcUaClientProvider opcUaClientProvider;
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
    public UaSlotBase(int slotId, SlotType type, NodeId tokenNodeId, int nameSpace, String opcUaName, OpcUaClientProvider clientProvider) {
        this.opcUaClientProvider = clientProvider;
        this.slotId = slotId;
        this.slotType = type;
        this.tokenId = tokenNodeId;
        this.opcUaName = opcUaName;
        this.namespace = nameSpace;
        this.slotName = "SLOT_" + slotId;
    }
    //Getters & Setters

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
}
