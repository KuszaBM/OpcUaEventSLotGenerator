package com.tlb.OpcUaSlotxGenerator.demo.process;

import ch.qos.logback.core.model.INamedModel;
import com.tlb.OpcUaSlotxGenerator.demo.slots.*;
import com.tlb.OpcUaSlotxGenerator.opcUa.*;
import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscriptionManager;
import org.eclipse.milo.opcua.sdk.core.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;


public class Process {
    private InThreadScheduler mainScheduler;
    private InThreadScheduler plcScheduler;
    private OpcUaSlotsProvider slotsProvider;
    Logger logger = LoggerFactory.getLogger(Process.class);
    short arg = 0;
    boolean slotLock = false;
    private final AtomicLong clientHandles = new AtomicLong(1L);

    public InThreadScheduler getMainScheduler() {
        return mainScheduler;
    }

    public Process(OpcUaSlotsProvider slotsProvider) {
        this.slotsProvider = slotsProvider;
        this.mainScheduler = new InThreadScheduler("Process");
        this.plcScheduler = slotsProvider.getScheduler();
    }

    public void readSlots() {
        try {
            logger.info("new slot call {}", slotsProvider.getReaderClient().getAddressSpace().getVariableNode(new NodeId(3, "\"PHS_OPC_COMM\".\"TEST_STRING\"")).readValue());
        } catch (UaException e) {
            throw new RuntimeException(e);
        }
    }


    public void start() {
        try {
//        SlotFromPlc slot1 = new SlotFromPlc(slotsProvider.getSlotToAdd().get(1));
//        slot1.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
//        Publisher<SlotCameraReq> p1 = slot1.makePublisher(new OpcUaReader<>(SlotCameraReq.class, slotsProvider.getSlotToAdd().get(1)), plcScheduler);
//        slot1.makeAutoAck();

//            mainScheduler.schedule(() -> {
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                logger.info("jjj aaa lll");
//            });
            for (int i = 1; i < 4; i++) {
                SlotFromPlc slot = new SlotFromPlc(this.slotsProvider.getSlotToAdd().get(i));
                slot.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
                Publisher<DecisionReq> p = slot.makePublisher(new OpcUaReader<>(DecisionReq.class, slotsProvider.getSlotToAdd().get(i)), plcScheduler);
                slot.makeAutoAck(mainScheduler);
                //Subscriber<DecisionResp> s = slot.makeSubscriber(new OpcUaWriter<>(DecisionResp.class, slotsProvider.getSlotToAdd().get(i)), mainScheduler);
                Flux<DecisionResp> newF = Flux.from(p).doOnNext((x) -> {
                    logger.info("new camera for track id - {} ", x.getTrackId());
                }).map((z) -> {
                    return new DecisionResp((short) 3);
                });
                //Flux<DecisionResp> neF =
                newF.doOnNext((o) -> {
                    logger.info("new decision to be sent");
                }).subscribe();

            }
            SlotToPlc slot21 = new SlotToPlc(slotsProvider.getSlotToAdd().get(21));
            slot21.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
            Processor<ToPlcResp, ToPlcReq> processor21 = slot21.makeProcessor(ToPlcResp.class, ToPlcReq.class, plcScheduler, mainScheduler);

            SlotToPlc slot22 = new SlotToPlc(slotsProvider.getSlotToAdd().get(22));
            slot22.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
            Processor<ToPlcResp, ToPlcReq> processor22 = slot22.makeProcessor(ToPlcResp.class, ToPlcReq.class, plcScheduler, mainScheduler);
            SlotToPlc slot23 = new SlotToPlc(slotsProvider.getSlotToAdd().get(23));
            slot23.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
            Processor<ToPlcResp, ToPlcReq> processor23 = slot23.makeProcessor(ToPlcResp.class, ToPlcReq.class, plcScheduler, mainScheduler);

            Flux.interval(Duration.ofMillis(420)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
                logger.info("new req to 21");
            }).subscribe(processor21);
            Flux.from(processor21).doOnNext((s) -> {
                logger.info("new resp from 21");
            }).subscribe();

                Flux.interval(Duration.ofMillis(400)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
                logger.info("new req to 22");
            }).subscribe(processor22);
            Flux.from(processor22).doOnNext((s) -> {
                logger.info("new resp from 22 - {}", s);
            }).subscribe();

            Flux.interval(Duration.ofMillis(430)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
                logger.info("new req to 23");
            }).subscribe(processor23);
            Flux.from(processor23).doOnNext((s) -> {
                logger.info("new resp from 23 - {}", s);
            }).subscribe();

//            Flux<Integer> a = Flux.fromArray(new Integer[]{1}).delayElements(Duration.ofMillis(1000));
//            a.doOnNext((z) -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                logger.info("dupcia - {}", z);
//            }).subscribeOn(mainScheduler).subscribe();
//            Flux<Integer> a1 = Flux.fromArray(new Integer[]{1}).delayElements(Duration.ofMillis(1100));
//            a1.doOnNext((z) -> {
//                logger.info("dupcia2 - {}", z);
//            }).subscribeOn(mainScheduler).subscribe();
//
//            mainScheduler.schedule(() -> {
//
//                logger.info("jjj aaa lll 1");
//            });
            mainScheduler.run();

        } catch (Exception e) {
            logger.info("a - ", e);
        }
    }
}
