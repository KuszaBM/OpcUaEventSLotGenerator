package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class BarcodeAssignWithDecisionResp {
    @OpcUaNode(name = "CAM_DECISION_DATA")
    private short decision;

    public BarcodeAssignWithDecisionResp(short decision) {
        this.decision = decision;
    }

    public short getDecision() {
        return decision;
    }

    public void setDecision(short decision) {
        this.decision = decision;
    }

    public BarcodeAssignWithDecisionResp() {
    }

}
