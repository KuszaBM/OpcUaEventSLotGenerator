package com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper;

import java.util.HashMap;
import java.util.Map;

public class SlotsKeeper {
    private final Map<Integer, SlotFromPlcUsable<?, ?>> slotsFromPlc = new HashMap<>();
    private final Map<Integer, SlotToPlcUsable<?, ?>> slotsToPlc = new HashMap<>();

    public SlotsKeeper() {
    }
    public void addSlotFromPlc(int slotId, SlotFromPlcUsable<?, ?> slot) {
        slotsFromPlc.put(slotId, slot);
    }
    public void addSlotToPlc(int slotId, SlotToPlcUsable<?, ?> slot) {
        slotsToPlc.put(slotId, slot);
    }
    public Map<Integer, SlotFromPlcUsable<?, ?>> getSlotsFromPlc() {
        return slotsFromPlc;
    }

    public Map<Integer, SlotToPlcUsable<?, ?>> getSlotsToPlc() {
        return slotsToPlc;
    }
}
