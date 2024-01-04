package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class OpcUaClientProvider {
    private OpcUaClient client;
    private boolean isConnected;
    private String address;
    private String opcUaName;
    private int nameSpace;
    Logger logger = LoggerFactory.getLogger(OpcUaClientProvider.class);

    public OpcUaClientProvider(String address, String opcUaName, int nameSpace) {
        this.address = address;
        this.opcUaName = opcUaName;
        this.nameSpace = nameSpace;
        this.isConnected = false;
        try {
            this.client = OpcUaClient.create(address);
        } catch (UaException e) {
            throw new RuntimeException(e);
        }
    }

    public void startConnection() {
        if(isConnected) {
            logger.warn("Phs already connected to: {}", address);
            return;
        }
        try {
            this.client.connect().get();
            logger.warn("OPCUA connection established");
            this.isConnected = true;
        } catch (Exception e) {
            logger.warn("Phs disconnected from OpcUa server: {}", address);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            startConnection();
        }

    }

    public OpcUaClient getClient() {
        return client;
    }
}
