package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class OpcUaClientProvider {
    private static OpcUaClientProvider instance;
    private OpcUaClient client;
    private OpcUaClient activatorClient;
    Thread con;
    private boolean isConnected;
    private String address;
    private String opcUaName;
    private int nameSpace;
    private boolean simulation;
    Logger logger = LoggerFactory.getLogger(OpcUaClientProvider.class);

    private OpcUaClientProvider(String address, String opcUaName, int nameSpace, boolean simulation) {
        this.address = address;
        this.opcUaName = opcUaName;
        this.nameSpace = nameSpace;
        this.isConnected = false;
        this.simulation = simulation;
    }
    public static OpcUaClientProvider getInstance(String address, String opcUaName, int nameSpace, boolean simulation) {
        if (instance == null) {
            instance = new OpcUaClientProvider(address, opcUaName, nameSpace, simulation);
        }
        return instance;
    }

    public void restartActivatorClient() {
        logger.info("Restarting activator client - {}", this.activatorClient);
        try {
            this.activatorClient.disconnect().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.info("error on dc");
        }
        try {
            this.activatorClient = OpcUaClient.create(address);
            this.activatorClient.connect().get();
            logger.info("New activator client crated - {}", this.activatorClient);
        } catch (UaException | InterruptedException | ExecutionException e) {
            logger.info("dupsko 12 - ", e);
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
                this.client.connect().get();
                this.activatorClient.connect().get();
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
        }, "OPC-CONNECTION");
        t.start();
    }

    public OpcUaClient getActivatorClient() {
        return activatorClient;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public OpcUaClient getClient() {
        return client;
    }
}
