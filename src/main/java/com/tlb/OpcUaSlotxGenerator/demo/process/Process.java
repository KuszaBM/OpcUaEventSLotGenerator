package com.tlb.OpcUaSlotxGenerator.demo.process;

import com.tlb.OpcUaSlotxGenerator.demo.slots.SlotFromPlcReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.SlotToPlcReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.SlotToPlcResp;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaReader;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.SlotFromPlc;
import com.tlb.OpcUaSlotxGenerator.opcUa.SlotToPlc;
import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;


public class Process {
    private InThreadScheduler mainScheduler;
    private InThreadScheduler plcScheduler;
    private OpcUaSlotsProvider slotsProvider;

    public Process(OpcUaSlotsProvider slotsProvider) {
        this.slotsProvider = slotsProvider;
        this.mainScheduler = new InThreadScheduler("Process");
        this.plcScheduler = slotsProvider.getScheduler();
    }

    public void start() {
        SlotToPlc slotToPlc = new SlotToPlc(this.slotsProvider.getSlotToAdd().get(0));
        SlotFromPlc slotFromPlc = new SlotFromPlc(this.slotsProvider.getSlotToAdd().get(1));

        Processor<SlotToPlcReq, SlotToPlcResp> processorToPlc = slotToPlc.makeProcessor(SlotToPlcReq.class, SlotToPlcResp.class);
        }
}
