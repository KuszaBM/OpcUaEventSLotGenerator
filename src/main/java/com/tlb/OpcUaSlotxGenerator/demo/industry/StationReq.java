package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class StationReq {
    int requestCode;

    @OpcUaConstructor
    public StationReq(@OpcUaNode(name = "REQUEST_CODE_DATA") int requestCode) {
        this.requestCode = requestCode;
    }

    public StationReq() {
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }
}
