package com.tlb.OpcUaSlotxGenerator.demo.industry;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import reactor.core.scheduler.Scheduler;

import java.util.*;
import java.util.function.Consumer;

public class TrackIdsPublisher implements Publisher<TrackId> {

	public TrackIdsPublisher(Logger logger, Scheduler scheduler) {
		this.logger = logger;
		this.scheduler = scheduler;
	}

	@Override
	public void subscribe(Subscriber<? super TrackId> s) {
		SubscriptionImpl sub = subscriptions.get(s);
		if (sub == null) {
			sub = new SubscriptionImpl(s);
			subscriptions.put(s, sub);
		}
		s.onSubscribe(sub);
	}

	public void trackIdRetrieved(TrackId tid) {
		if (! trackIdsInUse.add(tid))
			throw new IllegalStateException(tid.toString() + " already retrieved");
	}

	public void retrievingDone() {
		TrackId rnd = TrackId.random();
		TrackId tid = rnd;
		do {
			TrackId next = tid.nextTrackId();
			if (! trackIdsInUse.contains(next)) {
				lastTrackId = tid;
				return;
			}
			tid = next;
		} while (tid != rnd);
		throw new IllegalStateException("No free TrackIds at start");
	}

	public void trackIdReleased(TrackId tid) {
		if (! subscriptionsQueue.isEmpty()) {
			actionFor(tid);
		}
		else {
			trackIdsInUse.remove(tid);
		}
	}

	public final Consumer<TrackId> getReleasingConsumer() {
		return releasingConsumer;
	}

	private final void queueAction() {
		if (actionQueued) {
			logger.info("Action already queued");
			return;
		}
		actionQueued = true;
		scheduler.schedule(this::action);
	}

	private final void action() {
		actionQueued = false;
		while (! subscriptionsQueue.isEmpty()) {
			TrackId tid = getNextTrackId();
			if (tid == null)
				return;
			actionFor(tid);
		}
	}

	private final void actionFor(TrackId tid) {
		try {
			SubscriptionImpl s = subscriptionsQueue.remove();
			if (s.curentlyRequested < Long.MAX_VALUE)
				s.curentlyRequested--;
			scheduler.schedule(() -> {
				s.subscriber.onNext(tid);
			});

			if (s.curentlyRequested > 0) {
				subscriptionsQueue.add(s);
			}

		}
		catch (Exception e) {
			logger.warn("Exception processing subscription for " + tid, e);
		}
	}

	private final TrackId getNextTrackId() {
		Random r = new Random(12);
		TrackId last = lastTrackId;
		lastTrackId = lastTrackId.nextTrackId();
		return lastTrackId;
//		logger.warn("No more TrackId");
//		return null;
	}

	private final Consumer<TrackId> releasingConsumer = new Consumer<TrackId>() {
		@Override
		public void accept(TrackId t) {
			trackIdReleased(t);
		}
	};

	private final Map<Subscriber<? super TrackId>, SubscriptionImpl> subscriptions = new HashMap<>();
	private final Queue<SubscriptionImpl> subscriptionsQueue = new LinkedList<>();
	private final Set<TrackId> trackIdsInUse = new TreeSet<>();
	private final Logger logger;
	private final Scheduler scheduler;

	private boolean actionQueued = false;
	private TrackId lastTrackId = null;

	private final class SubscriptionImpl implements Subscription {

		SubscriptionImpl(Subscriber<? super TrackId> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public void request(long n) {
			if (n < 0)
				throw new IllegalArgumentException("Negative request");

			if (n == 0)
				return;

			boolean wasInQueue = curentlyRequested > 0;

			if (n == Long.MAX_VALUE || n >= Long.MAX_VALUE - curentlyRequested) {
				if (curentlyRequested < Long.MAX_VALUE)
					logger.warn("Request from {} becomes efectively unbounded and will be always consuming all possible TrackIds", subscriber.getClass().getName());
				curentlyRequested = Long.MAX_VALUE;
			}
			else
				curentlyRequested += n;

			if (! wasInQueue)
				subscriptionsQueue.add(this);
			queueAction();
		}

		@Override
		public void cancel() {
			subscriptions.remove(subscriber);
			subscriptionsQueue.remove(this);
		}

		final Subscriber<? super TrackId> subscriber;
		long curentlyRequested = 0;
	}
}
