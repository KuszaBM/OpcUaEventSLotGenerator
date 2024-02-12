package com.tlb.OpcUaSlotxGenerator.demo.process;

import ch.qos.logback.core.model.INamedModel;
import com.tlb.OpcUaSlotxGenerator.ServicesConnector;
import com.tlb.OpcUaSlotxGenerator.config.KafkaProcedureConfig;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

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
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.s;


public class Process {
    private InThreadScheduler mainScheduler;
    private InThreadScheduler plcScheduler;
    private OpcUaSlotsProvider slotsProvider;
    private ServicesConnector servicesConnector;
    Logger logger = LoggerFactory.getLogger(Process.class);
    private int ci = 0;
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
        ApplicationContext ctx2 = new AnnotationConfigApplicationContext(KafkaProcedureConfig.class);
        //this.servicesConnector = ctx2.getBean(ServicesConnector.class);;

    }

//    public void readSlots() {
//        try {
//            logger.info("new slot call {}", slotsProvider.getReaderClient().getAddressSpace().getVariableNode(new NodeId(3, "\"PHS_OPC_COMM\".\"TEST_STRING\"")).readValue());
//        } catch (UaException e) {
//            throw new RuntimeException(e);
//        }
//    }


    public void start() {
        try {
            for (int i = 1; i < 2; i++) {
                SlotFromPlc slot = new SlotFromPlc(this.slotsProvider.getSlotToAdd().get(i));
                slot.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
                Publisher<DecisionReq> p = slot.makePublisher(new OpcUaReader<>(DecisionReq.class, slotsProvider.getSlotToAdd().get(i)), plcScheduler);
                Subscriber<DecisionResp> s = slot.makeSubscriber(new OpcUaWriter<>(DecisionResp.class, slotsProvider.getSlotToAdd().get(i)), mainScheduler);
                //slot.makeAutoAck(mainScheduler);
                //Subscriber<DecisionResp> s = slot.makeSubscriber(new OpcUaWriter<>(DecisionResp.class, slotsProvider.getSlotToAdd().get(i)), mainScheduler);
                Flux.from(p).doOnNext((x) -> {
                    //servicesConnector.sendToService(x);
                    logger.info("new camera for track id - {} ", x.getTrackId());
                }).map((z) -> {
                    return new DecisionResp((short) 3);
                }).subscribe(s);
//                logger.info("Connector - {}", servicesConnector);
//                logger.info("Flux A - {}", servicesConnector.getOtputter().getDecisionsOutput());
//                logger.info("Flux B - {}", servicesConnector.streamDecisions());
//                servicesConnector.getOtputter().getDecisionsOutput().publishOn(mainScheduler).doOnNext((resp) -> {
//                    logger.info("service  1 response | tid: {}", resp.getDecision());
//                }).subscribe(s);
//                servicesConnector.streamDecisions().doOnNext((resp) -> {
//                    logger.info("service response | tid: {}", resp.getDecision());
//                }).subscribe();

                //Flux<DecisionResp> neF =

//                newF.doOnNext((o) -> {
//                    logger.info("new decision to be sent");
//                }).subscribe();

            }
            slotsProvider.getUaNotifierSingle().setInit(true);



            for (int i = 21; i < 22; i++) {
                this.ci = i;
                SlotToPlc slot = new SlotToPlc(slotsProvider.getSlotToAdd().get(i));
                slot.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
                Processor<ToPlcResp, ToPlcReq> processor = slot.makeProcessor(ToPlcResp.class, ToPlcReq.class, plcScheduler, mainScheduler);
                Flux.interval(Duration.ofMillis(10000 + (i-20)*50)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
                    logger.info("new req");
                }).subscribe(processor);
                Flux.from(processor).doOnNext((s) -> {
                    logger.info("new resp from ");
                }).subscribe();
            }

            mainScheduler.run();

        } catch (Exception e) {
            logger.info("a - ", e);
        }
    }
}
