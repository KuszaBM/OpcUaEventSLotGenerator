package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class BarcodeDecisionReq {
    private String barcode;
    @OpcUaConstructor
    public BarcodeDecisionReq(@OpcUaNode(name = "CAM_BARCODE_DATA") String barcode) {
        this.barcode = barcode;
    }

    public BarcodeDecisionReq() {
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
