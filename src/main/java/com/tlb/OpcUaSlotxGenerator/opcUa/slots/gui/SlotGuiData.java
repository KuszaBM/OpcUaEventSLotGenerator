package com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.SlotRequest;

import java.util.ArrayList;
import java.util.List;

public class SlotGuiData {
    private int id;
    private String direction;
    private boolean isOpen;
    private String currentData;
    private List<SlotRequest> requestsHistory;
    @JsonIgnore
    private SLotGuiPropagator propagator;

    public SlotGuiData(int id, String direction, boolean isOpen, String currentData, List<SlotRequest> requestsHistory) {
        this.id = id;
        this.direction = direction;
        this.isOpen = isOpen;
        this.currentData = currentData;
        this.requestsHistory = requestsHistory;
    }
    public SlotGuiData(int id, String direction, SLotGuiPropagator propagator) {
        this.id = id;
        this.direction = direction;
        this.propagator = propagator;
        this.isOpen = true;
        this.requestsHistory = new ArrayList<>();
    }
    public SlotGuiData() {
    }
    public SlotGuiData(int id, String direction, boolean isOpen, List<SlotRequest> requestsHistory) {
        this.id = id;
        this.direction = direction;
        this.isOpen = isOpen;
        this.requestsHistory = requestsHistory;
    }
    public void propagateChange() {
        propagator.propagateSlotChange(this);
    }
    public void newRequest(SlotRequest request) {
        this.isOpen = false;
        if(requestsHistory.size() > 2) {
            requestsHistory.remove(2);
        }
        List<SlotRequest> newList = new ArrayList<>();
        newList.add(request);
        request.setStart();
        newList.addAll(requestsHistory);
        requestsHistory = newList;
    }

    public void setDone() {
        this.isOpen = true;
        if (!requestsHistory.isEmpty())
            requestsHistory.get(0).setDone();
    }
    public void setResponse(String response) {
        if (!requestsHistory.isEmpty())
            requestsHistory.get(0).setResponseData(response);
    }
    public String getCurrentData() {
        return currentData;
    }

    public void setCurrentData(String currentData) {
        this.currentData = currentData;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public List<SlotRequest> getRequestsHistory() {
        return requestsHistory;
    }

    public void setRequestsHistory(List<SlotRequest> requestsHistory) {
        this.requestsHistory = requestsHistory;
    }
}
