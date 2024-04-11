package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.tlb.OpcUaSlotxGenerator.opcUa.slots.UaResponseListener;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpcSlotsActivator {
    short arg = 2;
    private int count;
    private OpcUaClientProvider clientProvider;
    private UaNotifierSingle uaNotifierSingle;
    private boolean callTimeout;
    private MethodTimeoutCounter methodTimeoutCounter;
    private Short[] initialCall;
    private AtomicBoolean listenTimeout = new AtomicBoolean(false);
    private AtomicBoolean timeout = new AtomicBoolean(false);
    Logger logger = LoggerFactory.getLogger(OpcSlotsActivator.class);

    public OpcSlotsActivator(OpcUaClientProvider clientProvider, UaNotifierSingle uaNotifierSingle) {
        this.clientProvider = clientProvider;
        this.uaNotifierSingle = uaNotifierSingle;
    }
    public void run() {
        count = 1;

        while (!uaNotifierSingle.isInit()) {
            try {
                logger.info("waiting for init of slots");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        initialSlotCall();
        //startingAsk();
        try {
            while (true) {
                callMet();
            }
        } catch (Exception e) {
            logger.info("error in method vcallin ", e);
            run();
        }

    }
    private void countTimeout() {
        Thread t = new Thread(() -> {
            this.timeout.set(this.methodTimeoutCounter.startTimeoutCounting());
        }, "METHOD TIMEOUT");
        t.start();
    }
    public void callMet() {
        NodeId objectId = null;
        NodeId methodId = null;
        if(uaNotifierSingle.isSimulation()) {
            objectId = new NodeId(2,"HelloWorld");
            methodId = new NodeId(2,"HelloWorld/Method");
        } else {
            objectId = new NodeId(3,"\"FB_OPC_COMMUNICATION_DB\"");
            methodId = new NodeId(3,"\"FB_OPC_COMMUNICATION_DB\".Method");
        }



        short zz = 2;
        short cc = 2;
        CallMethodRequest request = new CallMethodRequest(
                objectId, methodId, new Variant[]{new Variant(zz), new Variant(cc)});
        CallMethodResult result = null;
        try {
            logger.info("xc2b - Id = {}", count);
            this.methodTimeoutCounter = new MethodTimeoutCounter(count);
            countTimeout();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            result = clientProvider.getActivatorClient().call(request).get();
            if(!timeout.get()) {
                this.methodTimeoutCounter.stopTimeout();
            } else {
                logger.info("xc2b - Id = {} - timeout -> new call", count);
                logger.info("restarting client - {}", clientProvider.getActivatorClient());
                clientProvider.restartActivatorClient();
                logger.info("run on new client - {}", clientProvider.getActivatorClient());
                uaNotifierSingle.setInit(true);
                timeout.set(false);
                run();
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        StatusCode statusCode = result.getStatusCode();
        logger.info("xc2b - method response for ID - {} | SC - {}", count, statusCode);
        count ++;
        if(statusCode.isGood()) {
            Short[] resultArray = (Short[]) result.getOutputArguments()[1].getValue();
            logger.info("slots to activate | {}", Arrays.toString(resultArray));
            if(uaNotifierSingle.isInit()) {
                Set<Short> initialSet = new HashSet<>(List.of(resultArray));
                initialSet.addAll(List.of(initialCall));
                initialSet.remove((short) -1);
                resultArray = initialSet.toArray(new Short[0]);
                logger.info("Initial call - {}", Arrays.toString(resultArray));
                uaNotifierSingle.setInit(false);
            }
            if(resultArray.length < 1)
                return;
            if (resultArray[0] != -1) {
                uaNotifierSingle.runByMethod(resultArray);
            } else {
                logger.info("no slots to call");
            }
        } else {
            logger.info("Error calling OPC method - {}", statusCode);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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
        this.initialCall = out;
    }
}
