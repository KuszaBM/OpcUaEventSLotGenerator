package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class OpcSlotsFromPlcActivator {
    short arg = 2;
    private OpcUaClientProvider clientProvider;
    private UaNotifierSingle uaNotifierSingle;
    Logger logger = LoggerFactory.getLogger(OpcSlotsFromPlcActivator.class);

    public OpcSlotsFromPlcActivator(OpcUaClientProvider clientProvider, UaNotifierSingle uaNotifierSingle) {
        this.clientProvider = clientProvider;
        this.uaNotifierSingle = uaNotifierSingle;
    }

    public void startingAsk() {
        try {
            na(clientProvider.getClient(), arg).exceptionally(ex -> {
                logger.error("error invoking na()", ex);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Short[] x = new Short[1];
                x[0] = -1;
                return x;
            }).thenAccept(v -> {
                logger.info("na()={}", v);
                if(v[0] != -1) {
                    uaNotifierSingle.runByMethod(v);
                    logger.info("nextCall");
                } else {
                    logger.info("no slots to call");
                }
            });
        } catch (UaException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Short[]> na(OpcUaClient client, short input) throws UaException {
        NodeId objectId = new NodeId(3,"\"OPC_UA_TESTY_DB\"");
        NodeId methodId = new NodeId(3,"\"OPC_UA_TESTY_DB\".Method");
        short zz = 2;
        CallMethodRequest request = new CallMethodRequest(
                objectId, methodId, new Variant[]{new Variant(input), new Variant(zz)});

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();
            if (statusCode.isGood()) {
                //Variant[] array = (Variant[]) Arrays.stream(result.getOutputArguments()).toArray();
                Integer value = 1;
                //long a = (long) result.getOutputArguments()[0].getValue();
                Short[] a = (Short[]) result.getOutputArguments()[1].getValue();
                Short[] vv =  a;
                logger.info("result - {}", result.getOutputArguments()[1].getValue());
                startingAsk();
                return CompletableFuture.completedFuture(vv);
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
}
