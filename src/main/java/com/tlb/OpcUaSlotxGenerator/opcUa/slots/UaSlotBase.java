package com.tlb.OpcUaSlotxGenerator.opcUa.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaClientProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SLotGuiPropagator;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SlotGuiData;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
