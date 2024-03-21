package com.tlb.OpcUaSlotxGenerator.demo.industry;

public class StationResp {
    String status;

    public StationResp(String status) {
        this.status = status;
    }

    public StationResp() {
        this.status = "OK";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
