package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.google.common.collect.ImmutableList;
import com.tlb.OpcUaSlotxGenerator.exceptions.SlotCreationException;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.SlotFromPlc;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.SlotToPlc;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.SlotType;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.UaSlotBase;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SLotGuiPropagator;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SlotGuiData;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotFromPlcUsable;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotToPlcUsable;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotsKeeper;
import com.tlb.OpcUaSlotxGenerator.websocket.SinksHolder;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OpcUaSlotsProvider {
    private WebClient webClient;
    private SinksHolder sinksHolder;
    private Map<Integer, SlotFromPlc> slotFromPlcMap;
    private Map<Integer, SlotToPlc> slotToPlcMap;
    private static OpcUaSlotsProvider instance;
    private Map<Integer, UaSlotBase> slotToAdd;
    private SLotGuiPropagator propagator;
    private OpcUaClientProvider opcUaClientProvider;
    private boolean afterInit;
    private UaNotifierSingle uaNotifierSingle;
    private String address;
    private String opcUaName;
    private int nameSpace;
    private boolean simulation;
    private final SlotsKeeper slotsKeeper;
    private final Scheduler scheduler;
    private List<String> a = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(OpcUaSlotsProvider.class);
    private OpcUaSlotsProvider(String address, String opcUaName, int nameSpace, boolean simulation, OpcUaClientProvider opcUaClientProvider, SinksHolder sinksHolder) {
        this.address = address;
        this.opcUaName = opcUaName;
        this.nameSpace = nameSpace;
        this.sinksHolder = sinksHolder;
        this.opcUaClientProvider = opcUaClientProvider;
        this.slotToAdd = new HashMap<>();
        this.afterInit = false;
        this.scheduler = Schedulers.newSingle("PLC");
        this.slotFromPlcMap = new HashMap<>();
        this.slotToPlcMap = new HashMap<>();
        this.slotsKeeper = new SlotsKeeper();
        this.simulation = simulation;
    }
    public static OpcUaSlotsProvider getInstance(String address, String opcUaName, int nameSpace, boolean simulation, OpcUaClientProvider opcUaClientProvider, SinksHolder sinksHolder) {
        if (instance == null) {
            instance = new OpcUaSlotsProvider(address, opcUaName, nameSpace, simulation, opcUaClientProvider, sinksHolder);
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
        logger.info("initialig opc  - {}", afterInit);
        try {
            readServer();
        } catch (UaException | InterruptedException e) {
            logger.info("Śmierdzi kupskiem 113 1");
        }
        afterInit = true;
        //propagateALlSlots();
    }

    public void propagateALlSlots() {
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
        logger.info("jajca 12");
        if(nodeName.contains("TOKEN")) {
            String[] afterSplit = nodeName.split("_");
            for(int i = 0; i < afterSplit.length; i++ ) {
                if(afterSplit[i].equals("TOKEN")) {
                    int slotId = Integer.parseInt(afterSplit[i-1]);
                    if(slotId == 0) {
                        slotToAdd.put(slotId, new UaSlotBase(slotId, SlotType.ToPlc, cN.getNodeId(), nameSpace, opcUaName, opcUaClientProvider, propagator));
                    } else {
                        String slotDir = afterSplit[i+1].replace("\"", "");
                        SlotType slotType = slotDir.equals("IN") ? SlotType.ToPlc : SlotType.FromPlc;
                        slotToAdd.put(slotId, new UaSlotBase(slotId, slotType, cN.getNodeId(), nameSpace, opcUaName, opcUaClientProvider, propagator));
                    }
                }
            }
        }
        logger.info("jajca 13");
        if(cN.getNodeClass().getValue() == 2) {
            try {
                DataValue dV = opcUaClientProvider.getClient().getAddressSpace().getVariableNode(cN.getNodeId()).readValue();
                if(nodeName.contains("SLOT")) {
                            a.add(nodeName);
                }
                logger.info("Start value = {}", dV.getValue());
            } catch (UaException e) {
                logger.info("jebie gówbnem 15", e);
            }
        } else {
            logger.info("jajca 14");
            try {
                logger.info("jajca 15");
                List<? extends UaNode> list = cN.browseNodes();
                logger.info("jajca 16");
                if(list.size() > 0) {
                    logger.info("-------- inside [{}] | nodes - {} ----------", cN.getDisplayName(), list.size());
                    for (UaNode ccN : list) {
                        nodeShowUp(ccN);
                        Thread.sleep(30);
                    }
                }
            } catch (UaException e) {
                logger.info("Cannot read deeper inside this node");
            } catch (InterruptedException e) {
                logger.info("jebie gówbnem 151", e);
            }
        }
    }
    public <Req, Resp> SlotToPlcUsable<Req, Resp> makeSlotToPlc(int slotId, Scheduler mainScheduler, Class<Req> reqClass, Class<Resp> respClass) throws SlotCreationException {
        UaSlotBase slotBase = slotToAdd.get(slotId);
        if(slotBase == null)
            throw new SlotCreationException("No slot with id = " + slotId + " found on opc server");
        SlotToPlc slot = new SlotToPlc(slotBase, uaNotifierSingle);
        addSlotToPlc(slot);
        SlotToPlcUsable<Req, Resp> slotUsable = new SlotToPlcUsable<>(slot.makeProcessor(reqClass, respClass, scheduler, mainScheduler, reqClass, respClass), reqClass, respClass);
        slotsKeeper.addSlotToPlc(slotId, slotUsable);
        return slotUsable;
    }
    public <Req> SlotFromPlcUsable<Req, Object> makeAckOnlySlotFromPlc(int slotId, Scheduler mainScheduler, Class<Req> reqClass) throws SlotCreationException {
        UaSlotBase slotBase = slotToAdd.get(slotId);
        if(slotBase == null)
            throw new SlotCreationException("No slot with id = " + slotId + " found on opc server");
        SlotFromPlc slot = new SlotFromPlc(slotBase, uaNotifierSingle);
        addSlotFromPlc(slot);
        SlotFromPlcUsable<Req, Object> slotUsable = new SlotFromPlcUsable<>(
                slot.makePublisher(new OpcUaReader<>(reqClass, slotBase), scheduler, reqClass),
                slot.makeAckOnlySubscriber(mainScheduler),
                reqClass,
                null
                );
        slotsKeeper.addSlotFromPlc(slotId, slotUsable);
        return slotUsable;
    }
    public <Req> SlotFromPlcUsable<Req, Object> makeAutoAckSlotFromPlc(int slotId, Scheduler mainScheduler, Class<Req> reqClass) throws SlotCreationException {
        UaSlotBase slotBase = slotToAdd.get(slotId);
        if(slotBase == null)
            throw new SlotCreationException("No slot with id = " + slotId + " found on opc server");
        SlotFromPlc slot = new SlotFromPlc(slotBase, uaNotifierSingle);
        addSlotFromPlc(slot);
        SlotFromPlcUsable<Req, Object> slotUsable = new SlotFromPlcUsable<>(
                slot.makePublisher(new OpcUaReader<>(reqClass, slotBase), scheduler, reqClass),
                null,
                reqClass,
                null
        );
        slot.makeAutoAck(mainScheduler);
        slotsKeeper.addSlotFromPlc(slotId, slotUsable);
        return slotUsable;
    }
    public <Req, Resp> SlotFromPlcUsable<Req, Resp>
    makeTwoDirectionSlotFromPlc(int slotId, Scheduler mainScheduler, Class<Req> reqClass, Class<Resp> respClass) throws SlotCreationException, NoSuchMethodException {
        try {
            UaSlotBase slotBase = slotToAdd.get(slotId);
            if(slotBase == null)
                throw new SlotCreationException("No slot with id = " + slotId + " found on opc server");
            SlotFromPlc slot = new SlotFromPlc(slotBase, uaNotifierSingle);
            addSlotFromPlc(slot);
            SlotFromPlcUsable<Req, Resp> slotUsable = null;
                slotUsable = new SlotFromPlcUsable<>(
                        slot.makePublisher(new OpcUaReader<>(reqClass, slotBase), scheduler, reqClass),
                        slot.makeSubscriber(new OpcUaWriter<>(respClass, slotBase), mainScheduler),
                        reqClass,
                        respClass
                );
            slotsKeeper.addSlotFromPlc(slotId, slotUsable);
            return slotUsable;
        } catch (Exception e) {
            throw new SlotCreationException(e.getMessage());
        }
    }
    public <Req, Resp> SlotFromPlcUsable<Req, Resp>
    makeTwoDirectionTokenSlotFromPlc(int slotId, Scheduler mainScheduler, Class<Req> reqClass, Class<Resp> respClass) throws SlotCreationException, NoSuchMethodException {
        try {
            UaSlotBase slotBase = slotToAdd.get(slotId);
            if(slotBase == null)
                throw new SlotCreationException("No slot with id = " + slotId + " found on opc server");
            SlotFromPlc slot = new SlotFromPlc(slotBase, uaNotifierSingle);
            addSlotFromPlc(slot);
            slot.makeTwoDirectionToken();
            SlotFromPlcUsable<Req, Resp> slotUsable = null;
            slotUsable = new SlotFromPlcUsable<>(
                    slot.makePublisher(new OpcUaReader<>(reqClass, slotBase), scheduler, reqClass),
                    slot.makeSubscriber(new OpcUaWriter<>(respClass, slotBase), mainScheduler),
                    reqClass,
                    respClass
            );
            slotsKeeper.addSlotFromPlc(slotId, slotUsable);
            return slotUsable;
        } catch (Exception e) {
            throw new SlotCreationException(e.getMessage());
        }
    }
    public void testRead() throws UaException {
        UaVariableNode node = opcUaClientProvider.getClient().getAddressSpace().getVariableNode(Identifiers.Server_ServerStatus_StartTime);
        DataValue value = node.readValue();

        logger.info("StartTime={}", value.getValue().getValue());

        // asynchronous read request
        readServerStateAndTime(opcUaClientProvider.getClient()).thenAccept(values -> {
            DataValue v0 = values.get(0);
            DataValue v1 = values.get(1);

            logger.info("State={}", ServerState.from((Integer) v0.getValue().getValue()));
            logger.info("CurrentTime={}", v1.getValue().getValue());

        });
    }
    private CompletableFuture<List<DataValue>> readServerStateAndTime(OpcUaClient client) {
        List<NodeId> nodeIds = ImmutableList.of(
                Identifiers.Server_ServerStatus_State,
                Identifiers.Server_ServerStatus_CurrentTime);

        return client.readValues(0.0, TimestampsToReturn.Both, nodeIds);
    }
    public void readServer2() throws UaException {
        logger.info("Reading info from server OpcUa");
        String opcUaNameNodeFormat = "this.opcUaName";
        NodeId nodeId = new NodeId(2, opcUaNameNodeFormat);
        List<UaNode> nodesList = List.of(opcUaClientProvider.getClient().getAddressSpace().getNode(nodeId));
        for(UaNode n : nodesList) {
            logger.info("-------- node [{}] ---------", n.getDisplayName().getText());
            nodeShowUp(n);
        }
        logger.info("added Slots");
        for(Map.Entry<Integer, UaSlotBase> e : slotToAdd.entrySet()) {
            logger.info("SLOT {} - {}", e.getKey(), e.getValue().getSlotType());
        }
    }
    public void readServer() throws UaException, InterruptedException {
        logger.info("Reading info from server OpcUa");
        ;
        NodeId nodeId = new NodeId(this.nameSpace, this.opcUaName);
        logger.info("aaa - {}", nodeId);
        List<UaNode> nodesList = List.of(opcUaClientProvider.getClient().getAddressSpace().getNode(nodeId));
        for(UaNode n : nodesList) {
            logger.info("-------- node [{}] ---------", n.getDisplayName().getText());
            Thread.sleep(330);
            nodeShowUp(n);
        }
        logger.info("added Slots");
        for(Map.Entry<Integer, UaSlotBase> e : slotToAdd.entrySet()) {
            logger.info("SLOT {} - {}", e.getKey(), e.getValue().getSlotType());
        }
        for(String s : a) {
            System.out.println(s);
        }
    }
    public Map<Integer, UaSlotBase> getSlotToAdd() {
        return slotToAdd;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }
    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
        this.propagator = new SLotGuiPropagator(webClient, sinksHolder);
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

    public boolean isSimulation() {
        return simulation;
    }
}
