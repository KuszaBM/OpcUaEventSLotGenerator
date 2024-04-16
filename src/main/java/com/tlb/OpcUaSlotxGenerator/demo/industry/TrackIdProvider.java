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
            log.info("preparing nex trackId - given was - {}| rm size - {}", givenTrackId.getTrackId(), removedQueue.size());
            TrackId tempTrackIdHolder = removedQueue.poll();
            if(tempTrackIdHolder == null) {
                log.info("No available trackIds in rm queue - {}", removedQueue.size());
                tempTrackIdHolder = givenTrackId.nextTrackId();
                log.info("next trackId taken - {}", tempTrackIdHolder.getTrackId());
                while (trackIdsInUse.contains(tempTrackIdHolder)) {
                    log.info("TrackId {} already inUse - taking next", tempTrackIdHolder.getTrackId());
                    tempTrackIdHolder = tempTrackIdHolder.nextTrackId();
                    log.info("Next taken trackId {}", tempTrackIdHolder.getTrackId());
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
            } else {
                log.info("Taken trackId from rm - trackId {} | rm size {}", tempTrackIdHolder.getTrackId(), removedQueue.size());
            }
            log.info("new trackId {} ready to take", tempTrackIdHolder.getTrackId());
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
        while (inPreparation.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        TrackId givenTrackId = this.trackIdToTake;
        prepareNext(givenTrackId);
        trackIdsInUse.add(givenTrackId);
        log.info("Returning trackId - {}", givenTrackId.getTrackId());
        return givenTrackId;
    }
    public void trackIdRemove(TrackId trackId) {
        if(trackIdsInUse.remove(trackId)) {
            removedQueue.add(trackId);
            log.info("TrackId {} released - rm size {}", trackId.getTrackId(), removedQueue.size());
        } else {
            log.info("Unable to remove trackId {} - not in inUseList", trackId.getTrackId());
        }

    }

}
