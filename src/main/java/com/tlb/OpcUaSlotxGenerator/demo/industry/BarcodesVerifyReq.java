package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BarcodesVerifyReq {
    private List<String> barcodes = new ArrayList<>();

    @OpcUaConstructor
    public BarcodesVerifyReq(@OpcUaNode(name = "BARCODE_1_DATA") String barcode1, @OpcUaNode(name = "BARCODE_2_DATA") String barcode2, @OpcUaNode(name = "BARCODE_3_DATA") String barcode3, @OpcUaNode(name = "BARCODE_4_DATA") String barcode4) {
        barcodes.add(barcode1);
        barcodes.add(barcode2);
        barcodes.add(barcode3);
        barcodes.add(barcode4);
    }

    public FratDecision verify() {
        Set<String> set = new HashSet<>(barcodes);
        if(set.size() > 1) {
            return new FratDecision((short) 3001);
        } else {
            return new FratDecision((short) 1003);
        }
    }
}
