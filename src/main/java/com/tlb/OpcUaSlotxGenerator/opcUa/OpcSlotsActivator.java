package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class OpcSlotsActivator {
    short arg = 2;
    private OpcUaClientProvider clientProvider;
    private UaNotifierSingle uaNotifierSingle;
    Logger logger = LoggerFactory.getLogger(OpcSlotsActivator.class);

    public OpcSlotsActivator(OpcUaClientProvider clientProvider, UaNotifierSingle uaNotifierSingle) {
        this.clientProvider = clientProvider;
        this.uaNotifierSingle = uaNotifierSingle;
    }
    public void run() {
        while (!uaNotifierSingle.isInit()) {
            try {
                logger.info("waiting for init of slots");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        initialSlotCall();
        uaNotifierSingle.setInit(false);
        startingAsk();
    }
    public void reRun() {
        initialSlotCall();
        startingAsk();
    }
    public void startingAsk() {
        try {
            if(clientProvider.isConnected()) {
                slotsCall(clientProvider.getActivatorClient(), arg).exceptionally(ex -> {
                    logger.error("error invoking metchodcall()", ex);
                    if (ex instanceof CompletionException) {
                        logger.info("Connection error");
                        clientProvider.closeConnections();
                        logger.info("restarting connection");
                        clientProvider.startConnection();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        startingAsk();
                    }

                    Short[] x = new Short[1];
                    x[0] = -1;
                    return x;
                }).thenAccept(v -> {
                    logger.info("slots to activate | {}", v);
                    if (v[0] != -1) {
                        uaNotifierSingle.runByMethod(v);
                    } else {
                        logger.info("no slots to call");
                    }
                });
            } else {
                while (!clientProvider.isConnected()) {
                    logger.info("No call - waiting for connection");
                    Thread.sleep(1000);
                }
                startingAsk();
            }
        } catch (UaException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Short[]> slotsCall(OpcUaClient client, short input) throws UaException {
        NodeId objectId = new NodeId(3,"\"OPC_UA_TESTY_DB\"");
        NodeId methodId = new NodeId(3,"\"OPC_UA_TESTY_DB\".Method");
        short zz = 2;
        CallMethodRequest request = new CallMethodRequest(
                objectId, methodId, new Variant[]{new Variant(input), new Variant(zz)});

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();
            if (statusCode.isGood()) {
                Short[] a = (Short[]) result.getOutputArguments()[1].getValue();
                logger.info("result - {}", result.getOutputArguments()[1].getValue());
                startingAsk();
                return CompletableFuture.completedFuture(a);
            } else {
                StatusCode[] inputArgumentResults = result.getInputArgumentResults();
                for (int i = 0; i < inputArgumentResults.length; i++) {
                    logger.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
                }
                CompletableFuture<Short[]> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }
    public void initialSlotCall() {
        List<Short> slotsToCall = new ArrayList<>();
        Boolean token = false;
        logger.info("Slots size - {}", uaNotifierSingle.getSlots().values().size());
        for(UaResponseListener slot : uaNotifierSingle.getSlots().values()) {
            if(slot.getDirection()) {
                try {
                    token = (Boolean) clientProvider.getActivatorClient().getAddressSpace().getVariableNode(slot.getTokenNode()).readValue().getValue().getValue();
                    if(token) {
                        slotsToCall.add((short) slot.getSlotId());
                        logger.info("slot {} added to initial call", slot.getSlotId());
                    }
                } catch (UaException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Short[] out = slotsToCall.toArray(new Short[0]);
        uaNotifierSingle.runByMethod(out);
        uaNotifierSingle.setInit(true);
    }
}
