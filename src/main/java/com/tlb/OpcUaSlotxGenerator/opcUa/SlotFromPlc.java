package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SlotFromPlc implements UaResponseListener {

    public SlotFromPlc(UaSlotBase slotBase) {
        this.slotBase = slotBase;
    }

    public <Req> Publisher<Req> makePublisher(Supplier<? extends Req> requestReader, InThreadScheduler plcExecutor, Req ref) {
        if (this.reader != null)
            throw new IllegalStateException("makePublisher already called");
        this.plcExecutor = plcExecutor;
        isListening = true;
        SlotReader<Req> publisher = new SlotReader<>(requestReader);
        publisher.setReqReference(ref);
        this.reader = publisher;
        return publisher;
    }

    public <Resp> Subscriber<Resp> makeSubscriber(Consumer<? super Resp> responseWriter, InThreadScheduler fluxExecutor) {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.fluxExecutor = fluxExecutor;
        SubscribingSlotResponder<Resp> subscriber = new SubscribingSlotResponder<>(responseWriter);
        this.subscriber = subscriber;
        return subscriber;
    }

    public Subscriber<Void> makeAckOnlySubscriber1() {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.fluxExecutor = fluxExecutor;
        SubscribingSlotResponder<Void> subscriber = new SubscribingSlotResponder<>(null);
        this.subscriber = subscriber;
        return subscriber;
    }
    public Subscriber<Void> makeAckOnlySubscriber(InThreadScheduler fluxExecutor) {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.fluxExecutor = fluxExecutor;
        SubscribingSlotResponder<Void> subscriber = new SubscribingSlotResponder<>(null);
        this.subscriber = subscriber;
        return subscriber;
    }

    public void makeAutoAck() {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.subscriber = new TrivialResponder();
    }
    public void makeAutoAck(Scheduler scheduler) {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.fluxExecutor = scheduler;
        this.subscriber = new TrivialResponder();
    }

    private final void writePhsResponseAck() {
        slotBase.getSlotGuiData().setDone();
        slotBase.getSlotGuiData().propagateChange();
        Variant writeValue = new Variant(false);
        while(!slotBase.getOpcUaClientProvider().isConnected()) {
            logger.info("Plc not connected : slot {} blocked", slotBase.getSlotId());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    Logger logger = LoggerFactory.getLogger(SlotFromPlc.class);
    private ReaderBase reader;
    private SubscriberBase subscriber;
    private UaNotifierSingle uaNotifierSingle;
    private UaSlotBase slotBase;
    private boolean isListening;
    private final boolean direction = true;

    private Scheduler plcExecutor;
    private Scheduler fluxExecutor;

    @Override
    public void onTokenChange() {
        reader.onPlcRequest();
    }

    @Override
    public boolean isActivated() {
        return false;
    }

    @Override
    public void forceSlotUnlock() {
    }

    @Override
    public void forceSlotRequest(Object object) {

    }

    public UaNotifierSingle getUaNotifierSingle() {
        return uaNotifierSingle;
    }

    public void setUaNotifierSingle(UaNotifierSingle uaNotifierSingle) {
        this.uaNotifierSingle = uaNotifierSingle;
        uaNotifierSingle.addSlotToNotifier(this);
    }

    public void forceReq(Object req) {
        reader.forceRequest(req);
    }
    @Override
    public void setListening(boolean listening) {
        this.isListening = listening;
    }

    @Override
    public boolean getDirection() {
        return direction;
    }

    @Override
    public NodeId getTokenNode() {
        return slotBase.getTokenId();
    }

    @Override
    public int getSlotId() {
        return slotBase.getSlotId();
    }

    @Override
    public boolean isListening() {
        return isListening;
    }

    @Override
    public String getName() {
        return slotBase.getSlotName();
    }

    private interface ReaderBase {
        void onPlcRequest();
        void forceRequest(Object request);

    }

    private interface SubscriberBase {
        void requestedPhsResponse();
    }

    private final class SlotReader<Req> implements Publisher<Req>, ReaderBase {

        public SlotReader(Supplier<? extends Req> requestReader) {
            this.requestReader = requestReader;
        }
        private Req reqReference;

        public Req getReqReference() {
            return reqReference;
        }

        public void setReqReference(Req reqReference) {
            this.reqReference = reqReference;
        }

        @Override
        public void subscribe(Subscriber<? super Req> subscriber) {
            if (this.subscriber != null)
                throw new IllegalStateException("Multiple subscribe attempts");
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n < Long.MAX_VALUE)
                        requestsCount += n;
                    else
                        requestsCount = n;

                    proceedFluxRequests();
                }

                @Override
                public void cancel() {
                    throw new IllegalStateException("cancellation of PLC subscription is not allowed");
                }
            });

        }

        @Override
        public void onPlcRequest() {
            if (! request.compareAndSet(null, requestReader.get()))
                throw new IllegalStateException("PLC request before previous one has been read");
            fluxExecutor.schedule(this::proceedFluxRequests);
        }

        public void forceRequest(Object req) {
            ObjectMapper mapper = new ObjectMapper();
            String a = null;
            try {
                a = mapper.writeValueAsString(req);
                logger.info("new force reguest - {}", a);
            } catch (JsonProcessingException e) {
                logger.info("exc reguest - ", e);
            }
            Req request1 = (Req) req;
            try {
                request1 = (Req) mapper.readValue(a, reqReference.getClass());
            } catch (JsonProcessingException e) {
                logger.info("exc reguest - ", e);
            }
            if (! request.compareAndSet(null, request1))
                throw new IllegalStateException("PLC request before previous one has been read");
            fluxExecutor.schedule(this::proceedFluxRequests);
        }

        private final synchronized void proceedFluxRequests() {
            ObjectMapper mapper = new ObjectMapper();
            if (requestsCount > 0) {
                Req req = request.getAndSet(null);
                if (req != null) {
                    if (requestsCount < Long.MAX_VALUE)
                        requestsCount--;
                    SlotFromPlc.this.subscriber.requestedPhsResponse();
                    try {
                        slotBase.getSlotGuiData().newRequest(new SlotRequest(1,mapper.writeValueAsString(req)));
                        slotBase.getSlotGuiData().setCurrentData(mapper.writeValueAsString(req));
                        slotBase.getSlotGuiData().propagateChange();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    subscriber.onNext(req);
                }
            }
        }

        private final Supplier<? extends Req> requestReader;
        private volatile Subscriber<? super Req> subscriber = null;
        private volatile long requestsCount = 0;
        private final AtomicReference<Req> request = new AtomicReference<>(null);
    }

    private final class SubscribingSlotResponder<Resp> implements Subscriber<Resp>, SubscriberBase {

        public SubscribingSlotResponder(Consumer<? super Resp> responseWriter) {
            this.responseWriter = responseWriter;
        }

        @Override
        public void requestedPhsResponse() {
            if (this.subscription == null)
                throw new IllegalStateException("There is no subscrription");
            this.subscription.request(1);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (this.subscription != null)
                this.subscription.cancel();
            this.subscription = subscription;
            //TODO zrobić tak żeby kuba nie płakał
            subscription.request(1);
        }

        @Override
        public void onNext(Resp resp) {
            plcExecutor.schedule(()->{
                ObjectMapper mapper = new ObjectMapper();
                if (responseWriter != null)
                    responseWriter.accept(resp);
                try {
                    slotBase.getSlotGuiData().setResponse(mapper.writeValueAsString(resp));
                } catch (JsonProcessingException e) {
                    logger.info("exception - ", e);
                }
                writePhsResponseAck();
            });
        }

        @Override
        public void onError(Throwable t) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onComplete() {
            throw new IllegalStateException("onComplete is not allowed for SLOT-based Flux");
        }

        private final Consumer<? super Resp> responseWriter;
        private volatile Subscription subscription;
    }

    private final class TrivialResponder implements SubscriberBase {
        @Override
        public void requestedPhsResponse() {
            plcExecutor.schedule(SlotFromPlc.this::writePhsResponseAck);
        }

    }
}
