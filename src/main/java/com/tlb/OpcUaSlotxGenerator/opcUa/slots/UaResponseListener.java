package com.tlb.OpcUaSlotxGenerator.opcUa.slots;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public interface UaResponseListener {
    void onTokenChange();
    boolean getDirection();
    NodeId getTokenNode();
    String getName();
    int getSlotId();
}
