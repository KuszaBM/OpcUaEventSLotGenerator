package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;

public class TrackIdResponse {
    String status;
    @OpcUaConstructor
    public TrackIdResponse(){
        status = "TAKEN";
    };

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
