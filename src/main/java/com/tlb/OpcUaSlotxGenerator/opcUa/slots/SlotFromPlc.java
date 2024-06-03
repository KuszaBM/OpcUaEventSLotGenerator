package com.tlb.OpcUaSlotxGenerator.opcUa.slots;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlb.OpcUaSlotxGenerator.opcUa.UaNotifierSingle;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
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

    public SlotFromPlc(UaSlotBase slotBase, UaNotifierSingle notifierSingle) {
        this.slotBase = slotBase;
        this.uaNotifierSingle = notifierSingle;
        uaNotifierSingle.addSlotToNotifier(this);
    }

    public <Req> Publisher<Req> makePublisher(Supplier<? extends Req> requestReader, Scheduler plcExecutor, Class<Req> classReq) {
        if (this.reader != null)
            throw new IllegalStateException("makePublisher already called");
        this.plcExecutor = plcExecutor;
        SlotReader<Req> publisher = new SlotReader<>(requestReader);
        publisher.setReqReference(classReq);
        this.reader = publisher;
        return publisher;
    }

    public <Resp> Subscriber<Resp> makeSubscriber(Consumer<? super Resp> responseWriter, Scheduler fluxExecutor) {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.fluxExecutor = fluxExecutor;
        SubscribingSlotResponder<Resp> subscriber = new SubscribingSlotResponder<>(responseWriter);
        this.subscriber = subscriber;
        return subscriber;
    }

    public Subscriber<Object> makeAckOnlySubscriber(Scheduler fluxExecutor) {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.fluxExecutor = fluxExecutor;
        SubscribingSlotResponder<Object> subscriber = new SubscribingSlotResponder<>(null);
        this.subscriber = new TrivialResponder();
        return subscriber;
    }
    public void makeAutoAck(Scheduler scheduler) {
        if (this.subscriber != null)
            throw new IllegalStateException("makeSubscriber or makeAckOnlySubscriber or makeAutoAck already called");
        this.fluxExecutor = scheduler;
        this.subscriber = new TrivialResponder();
    }
    public void makeTwoDirectionToken() {
        this.twoDirectionToken = true;
    }

    private final void writePhsResponseAck() {
        logger.info("ac 12");
        //slotBase.writeSlotAck();
    }
    Logger logger = LoggerFactory.getLogger(SlotFromPlc.class);
    private ReaderBase reader;
    private SubscriberBase subscriber;
    private UaNotifierSingle uaNotifierSingle;
    private UaSlotBase slotBase;
    private final boolean direction = true;
    private Scheduler plcExecutor;
    private Scheduler fluxExecutor;
    private boolean twoDirectionToken = false;

    @Override
    public void onTokenChange() {
        reader.onPlcRequest();
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
        private Class<Req> reqClass;
        private final Supplier<? extends Req> requestReader;
        private volatile Subscriber<? super Req> subscriber = null;
        private volatile long requestsCount = 0;
        private final AtomicReference<Req> request = new AtomicReference<>(null);
        public SlotReader(Supplier<? extends Req> requestReader) {
            this.requestReader = requestReader;
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
            if(slotBase.getInAckMode().get()) {
                request.set(null);
                slotBase.setInAckMode(false);
            }
            if(twoDirectionToken) {
                request.set(null);
            }
            plcExecutor.schedule(() -> {
                slotBase.checkResponseState();
                Req req = requestReader.get();
                if (!request.compareAndSet(null, req)) {
                    logger.info("Exception on read or Duplicated request from PLC");
                    return;
                    //throw new IllegalStateException("PLC request before previous one has been read");
                }
                fluxExecutor.schedule(this::proceedFluxRequests);
            });

        }
        public void forceRequest(Object req) {
            //sim PlcReading
           // Req holder = requestReader.get();
        }
        private final synchronized void proceedFluxRequests() {
            if (requestsCount > 0) {
                Req req = request.getAndSet(null);
                if (req != null) {
                    if (requestsCount < Long.MAX_VALUE)
                        requestsCount--;
                    SlotFromPlc.this.subscriber.requestedPhsResponse();
                    subscriber.onNext(req);
                }
            }
        }
        public void setReqReference(Class<Req> reqClass) {
            this.reqClass = reqClass;
        }
    }

    private final class SubscribingSlotResponder<Resp> implements Subscriber<Resp>, SubscriberBase {

        private final Consumer<? super Resp> responseWriter;
        private volatile Subscription subscription;

        public SubscribingSlotResponder(Consumer<? super Resp> responseWriter) {
            this.responseWriter = responseWriter;
        }

        @Override
        public void requestedPhsResponse() {
            if (this.subscription == null)
                throw new IllegalStateException("There is no subscription");
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

    }
    private final class TrivialResponder implements SubscriberBase {
        @Override
        public void requestedPhsResponse() {
            logger.info("11 = 23");
            plcExecutor.schedule(slotBase::writeSlotAck);
        }

    }
}
