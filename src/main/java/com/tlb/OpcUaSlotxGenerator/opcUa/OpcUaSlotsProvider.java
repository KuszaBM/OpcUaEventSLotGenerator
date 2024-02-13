package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import com.tlb.OpcUaSlotxGenerator.websocket.SinksHolder;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpcUaSlotsProvider {
    private WebClient webClient;
    private SinksHolder sinksHolder;

    private SlotFromPlc testFromPLC;

    private Map<Integer, SlotFromPlc> slotFromPlcMap;
    private Map<Integer, SlotToPlc> slotToPlcMap;

    private static OpcUaSlotsProvider instance;
    private Map<Integer, UaSlotBase> slotToAdd;
    private SLotGuiPropagator propagator;
    private Map<Integer, SlotToPlc> slotsToPLc;
    private OpcUaClientProvider opcUaClientProvider;
    private boolean afterInit;
    private UaNotifierSingle uaNotifierSingle;
    private String address;
    private String opcUaName;
    private int nameSpace;
    private final InThreadScheduler scheduler;
    Logger logger = LoggerFactory.getLogger(OpcUaSlotsProvider.class);
    private OpcUaSlotsProvider(String address, String opcUaName, int nameSpace, OpcUaClientProvider opcUaClientProvider, SinksHolder sinksHolder) {
        this.address = address;
        this.opcUaName = opcUaName;
        this.nameSpace = nameSpace;
        this.sinksHolder = sinksHolder;
        this.opcUaClientProvider = opcUaClientProvider;
        this.slotToAdd = new HashMap<>();
        this.afterInit = false;
        this.scheduler = new InThreadScheduler("Plc");
        this.slotFromPlcMap = new HashMap<>();
        this.slotToPlcMap = new HashMap<>();
    }
    public static OpcUaSlotsProvider getInstance(String address, String opcUaName, int nameSpace, OpcUaClientProvider opcUaClientProvider, SinksHolder sinksHolder) {
        if (instance == null) {
            instance = new OpcUaSlotsProvider(address, opcUaName, nameSpace, opcUaClientProvider, sinksHolder);
        }
        return instance;
    }
    public SlotFromPlc getSlotFromPlc(int slotNo) {
        return slotFromPlcMap.get(slotNo);
    }
    public SlotToPlc getSlotToPlc(int slotNo) {
        return slotToPlcMap.get(slotNo);
    }
    public void addSlotFromPlc(SlotFromPlc slot) {
        slotFromPlcMap.put(slot.getSlotId(), slot);
    }
    public void addSlotToPlc(SlotToPlc slot) {
        slotToPlcMap.put(slot.getSlotId(), slot);
    }
    public void initialStart() {
        try {
            readServer();
        } catch (UaException e) {
            throw new RuntimeException(e);
        }
        afterInit = true;
        propagateALlSlots2();
        scheduler.run();
    }

    public void propagateALlSlots2() {
        List<SlotGuiData> guiData = new ArrayList<>();
        for (UaSlotBase base : slotToAdd.values()) {
            guiData.add(base.getSlotGuiData());
        }
        propagator.propagateALlSlots(guiData);
    }

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
                        slotToAdd.put(z, new UaSlotBase(z, SlotType.ToPlc, cN.getNodeId(), nameSpace, opcUaName, opcUaClientProvider, propagator));
                    } else {
                        SlotType slotType = afterSplit[i+1].equals("IN") ? SlotType.ToPlc : SlotType.FromPlc;
                        slotToAdd.put(z, new UaSlotBase(z, slotType, cN.getNodeId(), nameSpace, opcUaName, opcUaClientProvider, propagator));
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
    }
    public Map<Integer, UaSlotBase> getSlotToAdd() {
        return slotToAdd;
    }

    public InThreadScheduler getScheduler() {
        return scheduler;
    }
    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
        this.propagator = new SLotGuiPropagator(webClient, sinksHolder);
    }

    public SlotFromPlc getTestFromPLC() {
        return testFromPLC;
    }

    public void setTestFromPLC(SlotFromPlc testFromPLC) {
        this.testFromPLC = testFromPLC;
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
    public SLotGuiPropagator getPropagator() {
        return propagator;
    }
    public UaNotifierSingle getUaNotifierSingle() {
        return uaNotifierSingle;
    }
    public void setUaNotifierSingle(UaNotifierSingle uaNotifierSingle) {
        this.uaNotifierSingle = uaNotifierSingle;
    }
}
