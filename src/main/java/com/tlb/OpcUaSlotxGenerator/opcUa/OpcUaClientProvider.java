package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class OpcUaClientProvider {
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

    public OpcUaClientProvider(String address, String opcUaName, int nameSpace) {
        this.address = address;
        this.opcUaName = opcUaName;
        this.nameSpace = nameSpace;
        this.isConnected = false;
    }
    public void closeConnections() {
        isConnected = false;
        try {
            logger.info("jajko");
            c1.disconnect().get();
            logger.info("jaca");
            logger.info("jacychy 18");
        } catch (Exception e) {
            logger.info("ded - ", 3);
        }
        logger.info("DC DC DC DC DC 111");
    }

    public void startConnection() {
        logger.info("starting new connection");
        if(con != null) {
            logger.info("jajcarze 14");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.info("jajcarze 141");
        }
        Thread t = new Thread(() -> {
            if (isConnected) {
                logger.warn("Phs already connected to: {}", address);
                return;
            }
            logger.info("Trying to connect to {}", address);
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
