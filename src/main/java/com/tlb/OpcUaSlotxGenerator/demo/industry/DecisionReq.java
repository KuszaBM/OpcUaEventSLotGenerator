package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class DecisionReq {
    @OpcUaNode(name = "SORTER_DECISION_DATA")
    boolean[] decision;
    @OpcUaNode(name = "SORTER_DECISION_TID_DATA")
    short trackId;

    public DecisionReq(boolean[] decision, short trackId) {
        this.decision = decision;
        this.trackId = trackId;
    }

    public DecisionReq() {
    }

    public boolean[] getDecision() {
        return decision;
    }

    public void setDecision(boolean[] decision) {
        this.decision = decision;
    }

    public short getTrackId() {
        return trackId;
    }

    public void setTrackId(short trackId) {
        this.trackId = trackId;
    }
}
