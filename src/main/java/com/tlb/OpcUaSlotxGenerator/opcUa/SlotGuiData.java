package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class SlotGuiData {
    private int id;
    private String direction;
    private boolean isOpen;
    private List<SlotRequest> requestsHistory;
    @JsonIgnore
    private SLotGuiPropagator propagator;

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
        if(requestsHistory.size() > 9) {
            requestsHistory.remove(9);
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
