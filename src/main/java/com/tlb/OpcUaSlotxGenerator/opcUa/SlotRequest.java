package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SlotRequest {
    private int requestId;
    private String requestData;
    private Long proceedTime;
    @JsonIgnore
    private long start;

    public SlotRequest(int requestId, String requestData, Long proceedTime) {
        this.requestId = requestId;
        this.requestData = requestData;
        this.proceedTime = proceedTime;
    }

    public SlotRequest(int requestId, String requestData) {
        this.requestId = requestId;
        this.requestData = requestData;
        this.proceedTime = null;
        this.setStart();
    }
    public void setStart() {
        start = System.currentTimeMillis();
    }
    public void setDone() {
        this.proceedTime = System.currentTimeMillis() - start;
    }

    public SlotRequest() {
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    public Long getProceedTime() {
        return proceedTime;
    }

    public void setProceedTime(Long proceedTime) {
        this.proceedTime = proceedTime;
    }

    @Override
    public String toString() {
        return "SlotRequest{" +
                "requestId=" + requestId +
                ", requestData='" + requestData + '\'' +
                ", proceedTime=" + proceedTime +
                '}';
    }
}
