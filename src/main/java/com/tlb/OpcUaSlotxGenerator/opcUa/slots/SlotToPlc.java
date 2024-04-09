package com.tlb.OpcUaSlotxGenerator.opcUa.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaReader;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaWriter;
import com.tlb.OpcUaSlotxGenerator.opcUa.UaNotifierSingle;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class SlotToPlc implements UaResponseListener {
    private UaSlotBase slotBase;
    private UaResponseListener slot;
    private Scheduler plcExecutor;
    private Scheduler fluxExecutor;
    private UaNotifierSingle uaNotifierSingle;
    Logger log = LoggerFactory.getLogger(SlotToPlc.class);
    public UaSlotBase getSlotBase() {
        return slotBase;
    }
    public void setUaNotifierSingle(UaNotifierSingle uaNotifierSingle) {
        this.uaNotifierSingle = uaNotifierSingle;
        uaNotifierSingle.addSlotToNotifier(this);
    }
    public SlotToPlc(UaSlotBase slotBase, UaNotifierSingle notifierSingle) {
        this.slotBase = slotBase;
        this.uaNotifierSingle = notifierSingle;
        notifierSingle.addSlotToNotifier(this);
    }

    // requestWriter.accept(req) is called to write req to PLC, before sendRequestToPlc(), both from PLC thread
    // responseReader.get() is called to read response from PLC, from slot.onResponseFromPlc(), must not return null.
    public <Req, Resp> Processor<Req, Resp> makeProcessor(Class<? super Req> cls, Class<? extends Resp> cls1) {
        if (this.slot != null)
            throw new IllegalStateException("Slot already created");
        Consumer<? super Req> requestWriter = new OpcUaWriter<>(cls, slotBase);
        Supplier<? extends Resp> responseReader = new OpcUaReader<>(cls1,slotBase);
        Slot<Req, Resp> slot = new Slot<>(requestWriter, responseReader);
        this.slot = slot;
        return slot;
    }
    public <Req, Resp> Processor<Req, Resp> makeProcessor(Class<? super Req> cls, Class<? extends Resp> cls1, Scheduler plcExecutor, Scheduler fluxExecutor, Class<Req> reqClass, Class<Resp> respClass) {
        if (this.slot != null)
            throw new IllegalStateException("Slot already created");
        this.plcExecutor = plcExecutor;
        this.fluxExecutor = fluxExecutor;
        Consumer<? super Req> requestWriter = new OpcUaWriter<>(cls, slotBase);
        Supplier<? extends Resp> responseReader = new OpcUaReader<>(cls1,slotBase);
        Slot<Req, Resp> slot = new Slot<>(requestWriter, responseReader);
        slot.setReqClass(reqClass);
        slot.setRespClass(respClass);
        this.slot = slot;
        return slot;
    }

    public <Req, Resp> Processor<Req, Resp> makeProcessor(Consumer<? super Req> requestWriter, Supplier<? extends Resp> responseReader) {
        if (this.slot != null)
            throw new IllegalStateException("Slot already created");
        Slot<Req, Resp> slot = new Slot<>(requestWriter, responseReader);
        this.slot = slot;
        return slot;
    }

    @Override
    public void onTokenChange() {
        slot.onTokenChange();
    }


    @Override
    public boolean getDirection() {
        return false;
    }

    @Override
    public NodeId getTokenNode() {
        return slotBase.getTokenId();
    }

    @Override
    public String getName() {
        return slotBase.getSlotName();
    }

    @Override
    public int getSlotId() {
        return slotBase.getSlotId();
    }

    // Triggers PHS → PLC request sending procedure, called by PLC executor.
    // Once PLC confirms response is ready, slot.onResponseFromPlc() is to be called (from PLC thread, it is non-blocking)

    private final class Slot<Req, Resp> implements UaResponseListener, Processor<Req, Resp> {

        private final Consumer<? super Req> requestWriter;
        private final Supplier<? extends Resp> responseReader;
        private Subscription subscription;
        private Subscription publication;
        private boolean isListening;
        private final boolean direction = false;
        private boolean forced;
        private Subscriber<? super Resp> subscriber;
        private AtomicBoolean inRequest = new AtomicBoolean(false);
        private AtomicReference<Resp> response = new AtomicReference<>(null);
        private volatile long requestedFluxResponses = 0;
        private Class<Req> reqClass;
        private Class<Resp> respClass;

        public Class<Resp> getRespClass() {
            return respClass;
        }

        public void setRespClass(Class<Resp> respClass) {
            this.respClass = respClass;
        }

        public Class<Req> getReqClass() {
            return reqClass;
        }

        public void setReqClass(Class<Req> reqClass) {
            log.info("Set Slot request class - {}", reqClass);
            this.reqClass = reqClass;
        }

        public Slot(Consumer<? super Req> requestWriter, Supplier<? extends Resp> responseReader) {
            this.requestWriter = requestWriter;
            this.responseReader = responseReader;
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription != null)
                this.subscription.cancel();
            this.subscription = s;
            if (! inRequest.get() && this.subscription != null)
                this.subscription.request(1);
        }
        @Override
        public void onNext(Req request) {
            if (inRequest.getAndSet(true)) {
                log.info("SLOT {} - Too many requests simultaneously in progress", slotBase.getSlotId());
            }
            log.info("SLOT {} - on next scheduling", slotBase.getSlotId());
            plcExecutor.schedule(() -> {
                requestWriter.accept(request);
            });
        }

        @Override
        public void onTokenChange() {
            plcExecutor.schedule(() -> {
                log.info("SLOT {} - scheduled action started - 1", slotBase.getSlotId());
                if(slotBase.getInAckMode().get()) {
                    log.info("SLOT {} - new request Before old one write complete", slotBase.getSlotId());
                    response.set(null);
                    slotBase.setInAckMode(false);
                }
                log.info("SLOT {} - scheduled action started - 2", slotBase.getSlotId());
                slotBase.checkResponseState();
                log.info("SLOT {} - scheduled action started - 3", slotBase.getSlotId());
                fluxExecutor.schedule(() -> {
                    log.info("SLOT {} - scheduled action started - 4", slotBase.getSlotId());
                    Resp resp = responseReader.get();
                    if (! response.compareAndSet(null, resp)) {
                        log.info("Exception o read or Duplicated request from PLC");
                        log.info("New response from PLC but previous one has not been handled");
                        return;
                        }
                    log.info("SLOT {} - scheduled action started - 5", slotBase.getSlotId());
                    proceedFluxSubscription();
                });
            });
        }

        @Override
        public boolean getDirection() {
            return direction;
        }

        @Override
        public int getSlotId() {
            return slotBase.getSlotId();
        }

        @Override
        public NodeId getTokenNode() {
            return slotBase.getTokenId();
        }
        @Override
        public String getName() {
            return slotBase.getSlotName();
        }

        @Override
        public void onError(Throwable t) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onComplete() {
            throw new IllegalStateException("onComplete is not allowed for SLOT-based Flux");
        }

        private final synchronized void proceedFluxSubscription() {
            try {
                log.info("SLOT {} - scheduled action started - 5.1", slotBase.getSlotId());
                if (requestedFluxResponses > 0) {
                    log.info("SLOT {} - scheduled action started - 6", slotBase.getSlotId());
                    Resp resp = response.getAndSet(null);
                    if (resp != null) {
                        log.info("SLOT {} - scheduled action started - 7", slotBase.getSlotId());
                        if (requestedFluxResponses < Long.MAX_VALUE)
                            requestedFluxResponses--;
                        inRequest.set(false);
                        if(this.subscriber != null) {
                            log.info("SLOT {} - scheduled action started - 8", slotBase.getSlotId());
                            this.subscriber.onNext(resp);
                        }
                        log.info("SLOT {} - scheduled action started - 9", slotBase.getSlotId());
                        this.subscription.request(1);
                        log.info("SLOT {} - scheduled action started - 10", slotBase.getSlotId());
                    }
                }
            } catch (Exception e) {
                log.info("EX 14 - ", e);
            }

        }
        public void forceRequest(Req req) {
            onNext(req);
        }

        @Override
        public void subscribe(Subscriber<? super Resp> subscriber) {
            if (this.publication != null && this.subscriber != null) {
                subscriber.onComplete();
            }
            this.subscriber = subscriber;
            publication = new Subscription() {
                @Override
                public void request(long n) {
                    if (n < Long.MAX_VALUE)
                        requestedFluxResponses += n;
                    else
                        requestedFluxResponses = n;
                    log.info("SLOT {} - scheduled action started - 19", slotBase.getSlotId());
                    proceedFluxSubscription();
                }

                @Override
                public void cancel() {
                    if (Slot.this.subscriber == subscriber) {
                        requestedFluxResponses = 0;
                        Slot.this.subscriber = null;
                        publication = null;
                    }
                }
            };
            subscriber.onSubscribe(publication);
        }
    };
}

