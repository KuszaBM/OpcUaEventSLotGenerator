package com.tlb.OpcUaSlotxGenerator.opcUa.slots;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlb.OpcUaSlotxGenerator.opcUa.*;
import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
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
    public <Req, Resp> Processor<Req, Resp> makeProcessor(Class<? super Req> cls, Class<? extends Resp> cls1, InThreadScheduler plcExecutor, InThreadScheduler fluxExecutor, Class<Req> reqClass, Class<Resp> respClass) {
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
    public void forceSlotRequest(Object req) {
        slot.forceSlotRequest(req);
    }

    @Override
    public void forceSlotResponse(Object object) {
        slot.forceSlotResponse(object);
    }

    @Override
    public void onTokenChange() {
        slot.onTokenChange();
    }

    @Override
    public boolean isActivated() {
        return false;
    }

    @Override
    public void forceSlotUnlock() {
        slot.forceSlotUnlock();
    }

    @Override
    public void setListening(boolean listening) {

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
    public boolean isListening() {
        return false;
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
            if (inRequest.getAndSet(true))
                throw new IllegalStateException("Too many requests simultaneously in progress");

            while (!slotBase.getOpcUaClientProvider().isConnected()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                slotBase.getSlotGuiData().newRequest(new SlotRequest(1, mapper.writeValueAsString(request)));
                slotBase.getSlotGuiData().setCurrentData(mapper.writeValueAsString(request));
                slotBase.getSlotGuiData().propagateChange();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            plcExecutor.schedule(() -> {
                requestWriter.accept(request);
            });
        }

        @Override
        public void onTokenChange() {
            Resp resp = responseReader.get();
            if (! response.compareAndSet(null, resp))
                throw new IllegalStateException("New response from PLC but previous one has not been handled");
            ObjectMapper mapper = new ObjectMapper();
            try {
                slotBase.getSlotGuiData().setResponse(mapper.writeValueAsString(resp));
            } catch (JsonProcessingException e) {
                log.info("exception - ", e);
            }
            slotBase.getSlotGuiData().setDone();
            slotBase.getSlotGuiData().propagateChange();
            fluxExecutor.schedule(this::proceedFluxSubscription);
        }
        public void forceResponse(Resp resp) {
            if (! response.compareAndSet(null, resp))
                throw new IllegalStateException("New response from PLC but previous one has not been handled");
            ObjectMapper mapper = new ObjectMapper();
            try {
                slotBase.getSlotGuiData().setResponse(mapper.writeValueAsString(resp));
            } catch (JsonProcessingException e) {
                log.info("exception - ", e);
            }
            slotBase.getSlotGuiData().setDone();
            slotBase.getSlotGuiData().propagateChange();
            fluxExecutor.schedule(this::proceedFluxSubscription);

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
            Req req = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                String reqAsJson = null;
                reqAsJson = mapper.writeValueAsString(object);
                req = (Req) mapper.readValue(reqAsJson, reqClass);
                forceRequest(req);
            } catch (JsonProcessingException e) {
                log.info("exception Mapping - !!!!!");
            }

        }

        @Override
        public void forceSlotResponse(Object object) {
            Resp resp = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                String reqAsJson = null;
                reqAsJson = mapper.writeValueAsString(object);
                resp = (Resp) mapper.readValue(reqAsJson, respClass);
                forceResponse(resp);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
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
        public int getSlotId() {
            return slotBase.getSlotId();
        }

        @Override
        public NodeId getTokenNode() {
            return slotBase.getTokenId();
        }

        @Override
        public boolean isListening() {
            return isListening;
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
            if (requestedFluxResponses > 0) {
                Resp resp = response.getAndSet(null);
                if (resp != null) {
                    if (requestedFluxResponses < Long.MAX_VALUE)
                        requestedFluxResponses--;
                    inRequest.set(false);
                    if(this.subscriber != null)
                        this.subscriber.onNext(resp);
                    this.subscription.request(1);
                }
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

