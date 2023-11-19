package com.tlb.OpcUaSlotxGenerator.schedulers;

import org.slf4j.Logger;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractSingleQueueBaseScheduler implements Scheduler {

	protected AbstractSingleQueueBaseScheduler(String instance, Logger logger) {
		this.logger = logger;
		this.stringPrefix =  instance != null ? "Scheduler[ " + instance : "Scheduler[ ·";
	}

	@Override
	public Disposable schedule(Runnable task) {
		Objects.requireNonNull(task, "Runnable task");
		RunnableEntry re = new RunnableEntry(new Exception().getStackTrace(), task);
		queue.add(re);
		return re;
	}

	@Override
	public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
		Objects.requireNonNull(task, "Runnable task");
		Objects.requireNonNull(unit, "TimeUnit unit");
		if (delay < 0)
			throw new RejectedExecutionException("Scheduled with negative delay");
		long nextRun = System.currentTimeMillis() + unit.toMillis(delay);
		PeriodicOrDelayedEntry e = new PeriodicOrDelayedEntry(new Exception().getStackTrace(), task, nextRun);
		return e;
	}

	@Override
	public Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
		Objects.requireNonNull(task, "Runnable task");
		Objects.requireNonNull(unit, "TimeUnit unit");
		if (initialDelay < 0)
			throw new RejectedExecutionException("Scheduled with negative delay");
		long nextRun = System.currentTimeMillis() + unit.toMillis(initialDelay);
		long p = unit.toMillis(period);
		if (p <= 0)
			throw new RejectedExecutionException("Scheduled with non-positive period in [ms]");
		PeriodicOrDelayedEntry e = new PeriodicOrDelayedEntry(new Exception().getStackTrace(), task, nextRun, unit.toMillis(period));
		return e;
	}

	@Override
	public Worker createWorker() {
		return new WorkerImplementation();
	}

	@Override
	public String toString() {
		return  stringPrefix + stringBase() + " ]";
	}

	protected final String stringBase() {
		return ", #Q=" + queue.size() + ", #TO=" + timeouts.size();
	}

	protected final void loop() throws InterruptedException {
		while (true) {
			try {
				RunnableEntry re = queue.take();
				if (re.toDispose) {
					re.disposed = true;
				}
				else {
					try {
						re.runnable.run();
					}
					catch (Exception e) {
						HandlingException he = new HandlingException(re.exceptionMessage, e, true, true);
						he.setStackTrace(re.stackTrace);
						logger.warn("Unhandled exception", he);
					}
				}
			}
			catch (final InterruptedException ie) {
				logger.error("Thread innerrupted", ie);
				throw ie;
			}
			catch (Exception e) {
				logger.warn("Unhandled exception", e);
			}
		}
	}

	RunnableEntry nextEntry() throws InterruptedException  {
		long now = System.currentTimeMillis();
		while (true) {
			PeriodicOrDelayedEntry to = timeouts.peek();
			if (to == null)
				return queue.take();
			if (to.toDispose) {
				timeouts.remove(to);
				to.disposed = true;
				continue;
			}
			RunnableEntry ret = null;
			if (to.nextRun > now) {
				ret = queue.poll(to.nextRun - now, TimeUnit.MICROSECONDS);
				if (ret == null)
					continue;
				return ret;
			}
			else {
				ret = queue.poll();
			}
			timeouts.remove(to);
			if (to.toDispose) {
				to.disposed = true;
				if (ret != null)
					return ret;
				continue;
			}
			if (to.period != null) {
				queue.add(new RunnableEntry(to, ()-> {
					if (to.toDispose) {
						to.disposed = true;
						return;
					}
					to.nextRun = System.currentTimeMillis() + to.period;
					timeouts.add(to);
					to.runnable.run();
				}));
			}
			if (ret != null)
				return ret;
		}
	}

	private static String getClassName(Class<?> cls) {
		return cls.getName();
	}

	final Logger logger;

	BlockingQueue<RunnableEntry> queue = new LinkedBlockingQueue<>();
	PriorityQueue<PeriodicOrDelayedEntry> timeouts = new PriorityQueue<>();

	private final String stringPrefix;

	final class WorkerImplementation implements Worker {

		WorkerImplementation() {
			this.stringPrefix = "Worker for " + AbstractSingleQueueBaseScheduler.this.stringPrefix;
			this.tickDisposable = AbstractSingleQueueBaseScheduler.this.schedulePeriodically(tick, 1, 1, TimeUnit.MINUTES);
		}

		@Override
		public void dispose() {
			logger.info("{}: dispose", WorkerImplementation.this.toString());
			tickDisposable.dispose();
			for (Disposable d : disposables)
				d.dispose();
		}

		@Override
		public Disposable schedule(Runnable task) {
			Disposable d = AbstractSingleQueueBaseScheduler.this.schedule(task);
			disposables.add(d);
			return d;
		}

		@Override
		public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
			Disposable d = AbstractSingleQueueBaseScheduler.this.schedule(task, delay, unit);
			disposables.add(d);
			return d;
		}

		@Override
		public Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
			Disposable d = AbstractSingleQueueBaseScheduler.this.schedulePeriodically(task, initialDelay, period, unit);
			disposables.add(d);
			return d;
		}

		@Override
		public String toString() {
			return stringPrefix + AbstractSingleQueueBaseScheduler.this.stringBase() + ", #D=" + disposables.size() + " ]";
		}

		private final Runnable tick = new Runnable() {
			@Override
			public void run() {
				logger.info("{}: Tick", WorkerImplementation.this.toString());
			}
		};

		private final Set<Disposable> disposables = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Disposable, Boolean>()));
		private final String stringPrefix;
		private final Disposable tickDisposable;
	}


	static class RunnableEntry implements Disposable {

		RunnableEntry(StackTraceElement[] stackTrace, Runnable task) {
			this.runnable = task;
			this.stackTrace = stackTrace;
			this.exceptionMessage = "Exception from Runnable " + getClassName(runnable.getClass()) + " scheduled here";
		}

		RunnableEntry(PeriodicOrDelayedEntry periodicEntry, Runnable runnable) {
			this.runnable = runnable;
			this.stackTrace = periodicEntry.stackTrace;
			this.exceptionMessage = periodicEntry.exceptionMessage;
		}

		@Override
		public boolean isDisposed() {
			return disposed;
		}

		@Override
		public void dispose() {
			toDispose = true;
		}

		final Runnable runnable;
		final String exceptionMessage;
		final StackTraceElement[] stackTrace;
		volatile boolean toDispose = false;
		volatile boolean disposed = false;
	}	

	static final class PeriodicOrDelayedEntry implements Disposable, Comparable<PeriodicOrDelayedEntry> {

		public PeriodicOrDelayedEntry(StackTraceElement[] stackTrace, Runnable runnable, long nextRun) {
			this(stackTrace, runnable, nextRun, null);
		}

		public PeriodicOrDelayedEntry(StackTraceElement[] stackTrace, Runnable runnable, long nextRun, Long period) {
			super();
			this.nextRun = nextRun;
			this.period = period;
			this.stackTrace = stackTrace;
			this.runnable = runnable;
			this.exceptionMessage = "Exception from Runnable " + getClassName(runnable.getClass()) + " scheduled delayed or periodically here";
		}

		@Override
		public void dispose() {
			toDispose = true;
		}

		@Override
		public int compareTo(PeriodicOrDelayedEntry other) {
			if (this.nextRun > other.nextRun)
				return +1;
			if (this.nextRun < other.nextRun)
				return -1;
			return 0;
		}

		long nextRun;
		final Long period;
		final Runnable runnable;
		final String exceptionMessage;
		final StackTraceElement[] stackTrace;
		volatile boolean toDispose = false;
		volatile boolean disposed = false;
	}


}
