package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class FratDecision {

    @OpcUaNode(name = "CAM_DECISION_DATA")
    private short decision;

    public FratDecision(short decision) {
        this.decision = decision;
    }

    public FratDecision() {
    }

    public short getDecision() {
        return decision;
    }

    public void setDecision(short decision) {
        this.decision = decision;
    }
}
