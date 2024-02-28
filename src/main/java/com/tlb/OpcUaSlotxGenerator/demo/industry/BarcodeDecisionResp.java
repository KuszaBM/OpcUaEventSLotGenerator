package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class BarcodeDecisionResp {
    @OpcUaNode(name = "CAM_DECISION_DATA")
    private short decision;

    public BarcodeDecisionResp(short decision) {
        this.decision = decision;
    }

    public BarcodeDecisionResp() {
    }

    public short getDecision() {
        return decision;
    }

    public void setDecision(short decision) {
        this.decision = decision;
    }
}
