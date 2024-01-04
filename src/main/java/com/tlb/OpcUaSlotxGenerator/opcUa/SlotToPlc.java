package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class SlotToPlc implements UaResponseListener {
    private UaSlotBase slotBase;
    private UaResponseListener slot;
    private Scheduler plcExecutor; // TODO Provide valid executor
    private Scheduler fluxExecutor; // TODO provide valid executor

    private UaNotifierSingle uaNotifierSingle;
    public UaSlotBase getSlotBase() {
        return slotBase;
    }
    public void setUaNotifierSingle(UaNotifierSingle uaNotifierSingle) {
        this.uaNotifierSingle = uaNotifierSingle;
        uaNotifierSingle.addSlotToNotifier(this);
    }
    public SlotToPlc(UaSlotBase slotBase) {
        this.slotBase = slotBase;
        this.fluxExecutor = new Scheduler() {
            @Override
            public Disposable schedule(Runnable runnable) {
                runnable.run();
                return null;
            }

            @Override
            public Worker createWorker() {
                return new Worker() {
                    @Override
                    public Disposable schedule(Runnable runnable) {
                        runnable.run();
                        return null;
                    }
                    @Override
                    public void dispose() {

                    }
                };
            }
        };
        this.plcExecutor = new Scheduler() {
            @Override
            public Disposable schedule(Runnable runnable) {
                runnable.run();
                return null;
            }

            @Override
            public Worker createWorker() {
                return new Worker() {
                    @Override
                    public Disposable schedule(Runnable runnable) {
                        runnable.run();
                        return null;
                    }
                    @Override
                    public void dispose() {

                    }
                };
            }
        };
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
    public <Req, Resp> Processor<Req, Resp> makeProcessor(Class<? super Req> cls, Class<? extends Resp> cls1, InThreadScheduler plcExecutor, InThreadScheduler fluxExecutor) {
        if (this.slot != null)
            throw new IllegalStateException("Slot already created");
        this.plcExecutor = plcExecutor;
        this.fluxExecutor = fluxExecutor;
        Consumer<? super Req> requestWriter = new OpcUaWriter<>(cls, slotBase);
        Supplier<? extends Resp> responseReader = new OpcUaReader<>(cls1,slotBase);
        Slot<Req, Resp> slot = new Slot<>(requestWriter, responseReader);
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
        System.out.println("jajca 122");
        slot.onTokenChange();
    }

    @Override
    public boolean isActivated() {
        return false;
    }

    @Override
    public void setListening(boolean listening) {

    }

    @Override
    public boolean getDirection() {
        return true;
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
        private UaNotifier slotNotifier;
        private boolean isListening;
        private final boolean direction = true;
        private Subscriber<? super Resp> subscriber;
        private AtomicBoolean inRequest = new AtomicBoolean(false);
        private AtomicReference<Resp> response = new AtomicReference<>(null);
        private volatile long requestedFluxResponses = 0;
        public Slot(Consumer<? super Req> requestWriter, Supplier<? extends Resp> responseReader) {
            this.requestWriter = requestWriter;
            this.responseReader = responseReader;
//            try {
//                this.slotNotifier = new UaNotifier(false, this, slotBase, 100);
//            } catch (ExecutionException | InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }
        private final void sendRequestToPlc() {
            //version for many notifiers
            //slotNotifier.startListen();


            uaNotifierSingle.startListeningOnSLot(this);

            // TODO Wrrite this
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription != null)
                this.subscription.cancel();
            this.subscription = s;
            if (! inRequest.get() && this.subscription != null)
                this.subscription.request(1);
        }

        public UaNotifierSingle getUaNotifierSingle() {
            return uaNotifierSingle;
        }



        @Override
        public void onNext(Req request) {
            if (inRequest.getAndSet(true))
                throw new IllegalStateException("Too many requests simultaneously in progress");

            plcExecutor.schedule(() -> {
                requestWriter.accept(request);
                //sendRequestToPlc();
            });
        }

        @Override
        public void onTokenChange() {
            Resp resp = responseReader.get();
            if (! response.compareAndSet(null, resp))
                throw new IllegalStateException("New response from PLC but previous one has not been handled");
            fluxExecutor.schedule(this::proceedFluxSubscription);
        }

        @Override
        public boolean isActivated() {
            return false;
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
                    this.subscription.request(1);
                }
            }
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

