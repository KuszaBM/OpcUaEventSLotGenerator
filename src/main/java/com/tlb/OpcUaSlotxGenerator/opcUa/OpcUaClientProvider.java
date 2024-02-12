package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaClientProvider {
    private static OpcUaClientProvider instance;
    private OpcUaClient client;
    private OpcUaClient activatorClient;
    private UaClient c1;
    private UaClient c2;
    Thread con;
    private boolean isConnected;
    private String address;
    private String opcUaName;
    private int nameSpace;
    Logger logger = LoggerFactory.getLogger(OpcUaClientProvider.class);

    private OpcUaClientProvider(String address, String opcUaName, int nameSpace) {
        this.address = address;
        this.opcUaName = opcUaName;
        this.nameSpace = nameSpace;
        this.isConnected = false;
    }
    public static OpcUaClientProvider getInstance(String address, String opcUaName, int nameSpace) {
        if (instance == null) {
            instance = new OpcUaClientProvider(address, opcUaName, nameSpace);
        }
        return instance;
    }
    public void closeConnections() {
        isConnected = false;
        try {
            c1.disconnect().get();
        } catch (Exception e) {
            logger.info("ded - ", e);
        }
    }

    public void startConnection() {
        logger.info("Starting new connection");
        if(con != null) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Thread t = new Thread(() -> {
            if (isConnected) {
                logger.warn("Phs already connected to: {}", address);
                return;
            }
            logger.info("Trying to connect to {}...", address);
            try {
                this.client = OpcUaClient.create(address);
                this.activatorClient = OpcUaClient.create(address);
            } catch (UaException e) {
                throw new RuntimeException(e);
            }
            try {
                this.c1 = this.client.connect().get();
                this.c2 = this.activatorClient.connect().get();
                logger.warn("OPCUA connection established");
                con = Thread.currentThread();
                this.isConnected = true;
            } catch (Exception e) {
                isConnected = false;
                logger.warn("Phs disconnected from OpcUa server: {}", address);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                startConnection();
            }
        });
        t.start();
    }

    public OpcUaClient getActivatorClient() {
        return activatorClient;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public OpcUaClient getClient() {
        return client;
    }
}
