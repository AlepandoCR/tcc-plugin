package tcc.gamers.ai.spartan.monitor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Lightweight action monitor used by Spartan-related actions.
 *
 * This is a minimal implementation to satisfy compile-time references
 * from the plugin. It records simple counters and exposes a Snapshot
 * type. It deliberately keeps the API small to avoid coupling with
 * external monitoring libraries.
 */
public class SpartanActionMonitor {

	private final Logger logger;
	private final String name;

	private final AtomicInteger taskCount = new AtomicInteger();
	private final AtomicInteger awardChecks = new AtomicInteger();
	private final AtomicLong lastTaskValueBits = new AtomicLong();

	public SpartanActionMonitor(Logger logger, String name) {
		this.logger = logger;
		this.name = name;
	}

	public void recordTask(float magnitude) {
		taskCount.incrementAndGet();
		lastTaskValueBits.set(Float.floatToIntBits(magnitude));
	}

	public void recordAwardCheck() {
		awardChecks.incrementAndGet();
	}

	public Snapshot snapshot() {
		return new Snapshot(
				taskCount.get(),
				awardChecks.get(),
				Float.intBitsToFloat((int) lastTaskValueBits.get())
		);
	}

	public record Snapshot(int taskCount, int awardChecks, float lastTaskValue) {
	}

}


