package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OpcUaSlotsProvider {
    private Map<Integer, UaSlotBase> slotToAdd;
    private Map<Integer, SlotToPlc> slotsToPLc;
    private OpcUaClientProvider opcUaClientProvider;
    private boolean afterInit;
    private UaNotifierSingle uaNotifierSingle;
    private String address;
    private String opcUaName;
    private int nameSpace;
    //private OpcUaClient readerClient;
    private final InThreadScheduler scheduler;
    //private OpcUaClient client;
    Logger logger = LoggerFactory.getLogger(OpcUaSlotsProvider.class);
    private List<UaSlotBase> slots;
    private String opcAddress;

//    public OpcUaClient getReaderClient() {
//        return readerClient;
//    }

    public OpcUaSlotsProvider(String address, String opcUaName, int nameSpace, OpcUaClientProvider opcUaClientProvider) {
        this.address = address;
        this.opcUaName = opcUaName;
        this.nameSpace = nameSpace;
        this.opcUaClientProvider = opcUaClientProvider;
        this.slotToAdd = new HashMap<>();
        this.afterInit = false;
        this.scheduler = new InThreadScheduler("Plc");
        this.slots = new ArrayList<>();
        logger.info("jkaj");
    }
//    public void reconnect() {
//        logger.info("Trying to reconnect to OPC");
//        try {
//            this.client = OpcUaClient.create(address);
//            this.client.connect().get();
//        } catch (UaException | InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }
//    }
//    public void startConnection() {
//        logger.info("Trying to establish connection to OPCUA server");
//        try {
//            this.client.connect().get();
//            this.readerClient.connect().get();
//            logger.warn("OPCUA connection established");
//            readServer();
//            afterInit = true;
//            scheduler.run();
//        } catch (Exception e) {
//            logger.info("OPC UA - DC", e);
//
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException ex) {
//                throw new RuntimeException(ex);
//            }
//            startConnection();
//        }
//    }
    public void initialStart() {
        try {
            readServer();
        } catch (UaException e) {
            throw new RuntimeException(e);
        }
        afterInit = true;
        scheduler.run();
    }

//    private void startHb(NodeId hbToken) {
//        Thread hb = new Thread(() -> {
//            boolean b;
//            while (true) {
//                try {
//                    b = (Boolean) opcUaClientProvider.getClient().getAddressSpace().getVariableNode(hbToken).readValue().getValue().getValue();
//                } catch (UaException e) {
//                    throw new RuntimeException(e);
//                }
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                if (b) {
//                    Variant writeValue = new Variant(false);
//                    DataValue dataValue = DataValue.valueOnly(writeValue);
//                    CompletableFuture<StatusCode> status = client.writeValue(hbToken, dataValue);
//                    try {
//                        if(status.get().isBad()) {
//                            logger.info("OPC beep bad");
//                        }
//                    } catch (InterruptedException | ExecutionException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                }
//            }
//        });
//        logger.info("Starting heartbeat Thread");
//        hb.start();
//    }
    public void nodeShowUp(UaNode cN) {
        logger.info("--------------------------------");
        logger.info("{} | {}", cN.getDisplayName().getText(), cN.getNodeId());

        String nodeName = cN.getDisplayName().getText();
        if(nodeName == null)
            return;
        if(nodeName.contains("TOKEN")) {
            String[] afterSplit = nodeName.split("_");
            for(int i = 0; i < afterSplit.length; i++ ) {
                if(afterSplit[i].equals("TOKEN")) {
                    int z = Integer.parseInt(afterSplit[i-1]);

                    if(z == 0) {
                        slotToAdd.put(z, new UaSlotBase(z, SlotType.ToPlc, cN.getNodeId(), nameSpace, opcUaName, opcUaClientProvider));
                        //startHb(cN.getNodeId());

                    } else {
                        SlotType slotType = afterSplit[i+1].equals("IN") ? SlotType.ToPlc : SlotType.FromPlc;
                        slotToAdd.put(z, new UaSlotBase(z, slotType, cN.getNodeId(), nameSpace, opcUaName, opcUaClientProvider));
                    }

                }
            }

        }
        if(cN.getNodeClass().getValue() == 2) {
            try {
                DataValue dV = opcUaClientProvider.getClient().getAddressSpace().getVariableNode(cN.getNodeId()).readValue();
                logger.info("Start value = {}", dV.getValue());
            } catch (UaException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                if(cN.browseNodes().size() > 0) {
                    logger.info("-------- inside [{}] | nodes - {} ----------", cN.getDisplayName(), cN.browseNodes().size());
                    for (UaNode ccN : cN.browseNodes()) {
                        nodeShowUp(ccN);
                    }
                }
            } catch (UaException e) {
                logger.info("Cannot read deeper inside this node");
                throw new RuntimeException(e);
            }
        }

    }
    public void readServer() throws UaException {
        logger.info("Reading info from server OpcUa");
        String opcUaNameNodeFormat = "\"" + this.opcUaName + "\"";
        NodeId nodeId = new NodeId(3, opcUaNameNodeFormat);
        List<UaNode> nodesList = List.of(opcUaClientProvider.getClient().getAddressSpace().getNode(nodeId));
        for(UaNode n : nodesList) {
            logger.info("-------- node [{}] ---------", n.getDisplayName().getText());
            nodeShowUp(n);
        }
        slotsToPLc = new HashMap<>();
        logger.info("added Slots");
        for(Map.Entry<Integer, UaSlotBase> e : slotToAdd.entrySet()) {
            logger.info("SLOT {} - {}", e.getKey(), e.getValue().getSlotType());
            if(e.getValue().getSlotType().equals(SlotType.ToPlc)) {
                slotsToPLc.put(e.getKey(), new SlotToPlc(e.getValue()));
            }
        }
        logger.info("13 - 13 - 13 - 13");
    }


    public Map<Integer, UaSlotBase> getSlotToAdd() {
        return slotToAdd;
    }

    public InThreadScheduler getScheduler() {
        return scheduler;
    }

    public boolean isAfterInit() {
        return afterInit;
    }
    public static byte[] convertUByte(UByte[] ubajtki) {
        byte[] ret = new byte[ubajtki.length];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = ubajtki[i].byteValue();
        }
        return ret;
    }
    public UaNotifierSingle getUaNotifierSingle() {
        return uaNotifierSingle;
    }
    public void setUaNotifierSingle(UaNotifierSingle uaNotifierSingle) {
        this.uaNotifierSingle = uaNotifierSingle;
    }
}
