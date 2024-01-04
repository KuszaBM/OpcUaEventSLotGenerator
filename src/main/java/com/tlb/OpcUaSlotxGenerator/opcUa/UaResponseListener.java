package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public interface UaResponseListener {
    void onTokenChange();
    boolean isActivated();
    void setListening(boolean listening);
    boolean getDirection();
    NodeId getTokenNode();
    boolean isListening();
    String getName();
    int getSlotId();
}
