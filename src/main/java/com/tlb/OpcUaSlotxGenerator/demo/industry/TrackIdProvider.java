package com.tlb.OpcUaSlotxGenerator.demo.industry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackIdProvider {
    Logger log = LoggerFactory.getLogger(TrackIdProvider.class);
    private Set<TrackId> trackIdsInUse;
    private TrackId trackIdToTake;
    private LinkedBlockingQueue<TrackId> removedQueue = new LinkedBlockingQueue<>();

    private AtomicBoolean inPreparation = new AtomicBoolean(false);

    private void prepareNext(TrackId givenTrackId) {
        inPreparation.set(true);
        Thread preparingThread = new Thread(() -> {
            TrackId tempTrackIdHolder = removedQueue.poll();
            if(tempTrackIdHolder == null) {
                tempTrackIdHolder = givenTrackId.nextTrackId();
                while (trackIdsInUse.contains(tempTrackIdHolder)) {
                    tempTrackIdHolder = tempTrackIdHolder.nextTrackId();
                    if(tempTrackIdHolder.equals(givenTrackId)) {
                        try {
                            log.info("No TrackId available - waiting for removal");
                            tempTrackIdHolder = removedQueue.take();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                        break;
                }
            }
            this.trackIdToTake = tempTrackIdHolder;
            this.inPreparation.set(false);
        });
        preparingThread.start();

    }

    public TrackIdProvider() {
        trackIdsInUse = new HashSet<>();
        //temp solution
        TrackId initTid = new TrackId(1);
        this.trackIdToTake = initTid;
    }
    public synchronized TrackId takeNewTrackId() {
        if(inPreparation.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        TrackId givenTrackId = this.trackIdToTake;
        prepareNext(givenTrackId);
        trackIdsInUse.add(givenTrackId);

        return givenTrackId;
    }
    public void trackIdRemove(TrackId trackId) {
        if(trackIdsInUse.remove(trackId))
            removedQueue.add(trackId);
    }

}
