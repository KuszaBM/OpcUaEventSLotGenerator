package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class DecisionResp {
    private short decision;

    public DecisionResp(short decision) {
        this.decision = decision;
    }

    public short getDecision() {
        return decision;
    }

    public void setDecision(short decision) {
        this.decision = decision;
    }
}
