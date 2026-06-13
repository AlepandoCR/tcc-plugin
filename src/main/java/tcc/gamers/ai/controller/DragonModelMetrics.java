package tcc.gamers.ai.controller;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Session-level metrics aggregator for the shared RSAC model.
 * */
public final class DragonModelMetrics {

    /** How many ticks per snapshot interval. 200 ticks = 10s @ 20TPS. */
    private static final int INTERVAL_TICKS = 200;

    /** Maximum number of interval snapshots retained in memory. */
    private static final int MAX_HISTORY_INTERVALS = 100; // = last 20000 ticks = ~17 min


    private final Object intervalLock = new Object(); // guards only the interval flush
    private int  intervalTickCount   = 0;
    private double intervalRewardSum = 0.0;
    private double intervalBounceSum = 0.0;
    private double intervalAlignSum  = 0.0;

    /** Total ticks recorded across all dragons across the entire session. */
    private final LongAdder sessionTicks = new LongAdder();

    /** Sum of all reward values recorded. */
    private double sessionRewardSum = 0.0; // guarded by intervalLock

    /** Total bounce ratio sum (for average). */
    private double sessionBounceSum = 0.0;

    /** Total alignment sum (for average). */
    private double sessionAlignSum = 0.0;

    /** Total ticks where bounceRatio exceeded the bounce threshold. */
    private final LongAdder bounceTicks = new LongAdder();

    /** Number of arrival events (distance < threshold, any dragon). */
    private final AtomicInteger sessionArrivals = new AtomicInteger();

    /** Number of timeout replacements (any dragon). */
    private final AtomicInteger sessionTimeouts = new AtomicInteger();

    /** Number of out-of-bounds forced replacements. */
    private final AtomicInteger sessionOutOfBounds = new AtomicInteger();

    /** Number of distinct dragons spawned during this session. */
    private final AtomicInteger dragonsSpawned = new AtomicInteger();

    /** Peak interval average reward seen. */
    private volatile double peakIntervalReward = Double.NEGATIVE_INFINITY;

    /** Worst interval average reward seen. */
    private volatile double worstIntervalReward = Double.POSITIVE_INFINITY;

    private final ArrayDeque<Double> intervalHistory = new ArrayDeque<>();

    /**
     * Records one tick of data from any dragon.
     * Called by {@link DragonFlightMetricsHelper#calculateAndTrackReward()} for
     * every dragon every tick.
     *
     * @param reward      The full reward value for this tick (including all terms).
     * @param bounceRatio The bounce ratio computed this tick [0.0, 1.0].
     * @param alignment   The look-to-target alignment this tick [-1.0, 1.0].
     * @param isBouncing  {@code true} if bounceRatio exceeded the penalty threshold.
     * @param isArrival   {@code true} if this tick triggered the arrival bonus.
     */
    public void record(
            double reward,
            double bounceRatio,
            double alignment,
            boolean isBouncing,
            boolean isArrival
    ) {
        synchronized (intervalLock) {
            intervalTickCount++;
            intervalRewardSum += reward;
            intervalBounceSum += bounceRatio;
            intervalAlignSum  += alignment;
            sessionRewardSum  += reward;
            sessionBounceSum  += bounceRatio;
            sessionAlignSum   += alignment;

            if (isArrival)   sessionArrivals.incrementAndGet();
            if (isBouncing)  bounceTicks.increment();

            if (intervalTickCount >= INTERVAL_TICKS) {
                flushInterval();
            }
        }
        sessionTicks.increment();
    }

    /** Called by the session when a dragon is replaced due to timeout. */
    public void notifyTimeout()    { sessionTimeouts.incrementAndGet(); }

    /** Called by the session when a dragon is replaced due to going out of bounds. */
    public void notifyOutOfBounds(){ sessionOutOfBounds.incrementAndGet(); }

    /** Called by the session when a new dragon is spawned. */
    public void notifyDragonSpawned() { dragonsSpawned.incrementAndGet(); }


    /**
     * Commits the current interval's averages to {@link #intervalHistory} and
     * resets the rolling accumulators. Must be called with {@link #intervalLock} held.
     */
    private void flushInterval() {
        double avgReward = intervalRewardSum / intervalTickCount;
        double avgBounce = intervalBounceSum / intervalTickCount;
        double avgAlign  = intervalAlignSum  / intervalTickCount;

        if (intervalHistory.size() >= MAX_HISTORY_INTERVALS) intervalHistory.pollFirst();
        intervalHistory.addLast(avgReward);

        if (avgReward > peakIntervalReward)  peakIntervalReward  = avgReward;
        if (avgReward < worstIntervalReward) worstIntervalReward = avgReward;

        intervalTickCount = 0;
        intervalRewardSum = 0.0;
        intervalBounceSum = 0.0;
        intervalAlignSum  = 0.0;
    }

    /**
     * Dumps the full session diagnostic panel.
     * Called once by {@link tcc.gamers.ai.queue.DragonQueueManager#shutdown()}.
     * </pre>
     */
    public void dumpSessionReport(@NotNull ComponentLogger logger) {
        synchronized (intervalLock) {
            if (intervalTickCount > 0) flushInterval();
        }

        long ticks = sessionTicks.sum();
        if (ticks == 0) {
            logger.info(Component.text("[DragonModelMetrics] No ticks recorded — nothing to report."));
            return;
        }

        double perTick = sessionRewardSum / ticks;
        double bounceAvg = (sessionBounceSum / ticks) * 100.0;
        double alignAvg = sessionAlignSum / ticks;
        long   bounceTix = bounceTicks.sum();
        double bounceTixPct = (bounceTix * 100.0) / ticks;

        int arrivals = sessionArrivals.get();
        int timeouts = sessionTimeouts.get();
        int oob = sessionOutOfBounds.get();
        int total = arrivals + timeouts + oob;
        double arrRate = total > 0 ? (arrivals * 100.0 / total) : 0.0;

        String arrVerdict = verdict(arrRate,  60, 30, "✔ GOOD", "⚠ LOW — raise ARRIVAL_BONUS or ARRIVAL_RADIUS", "✘ POOR — model never learns task");
        String bounceVerdict = verdictInv(bounceAvg, 20, 40, "✔ ACCEPTABLE", "⚠ MODERATE — raise BOUNCE_PENALTY_WEIGHT", "✘ HIGH — still exploiting pitch-thrust");
        String alignVerdict = verdict(alignAvg * 100, 50, 20, "✔ GOOD", "⚠ WEAK — alignment signal too small", "✘ POOR — rarely facing target");

        String trendBlock = buildTrendBlock();

        var mm = MiniMessage.miniMessage();
        TagResolver tagResolver = TagResolver.builder()
                .resolver(Placeholder.parsed("ticks",        String.valueOf(ticks)))
                .resolver(Placeholder.parsed("seconds",      String.format("%.0f", ticks / 20.0)))
                .resolver(Placeholder.parsed("hours",        String.format("%.2f", ticks / 72000.0)))
                .resolver(Placeholder.parsed("spawned",      String.valueOf(dragonsSpawned.get())))
                .resolver(Placeholder.parsed("reward_total", String.format("%.1f", sessionRewardSum)))
                .resolver(Placeholder.parsed("reward_color", perTick >= 0 ? "green" : "red"))
                .resolver(Placeholder.parsed("per_tick",     String.format("%.4f", perTick)))
                .resolver(Placeholder.parsed("peak",         String.format("%.2f", peakIntervalReward == Double.NEGATIVE_INFINITY ? 0 : peakIntervalReward)))
                .resolver(Placeholder.parsed("worst",        String.format("%.2f", worstIntervalReward == Double.POSITIVE_INFINITY ? 0 : worstIntervalReward)))
                .resolver(Placeholder.parsed("arrivals",     String.valueOf(arrivals)))
                .resolver(Placeholder.parsed("timeouts",     String.valueOf(timeouts)))
                .resolver(Placeholder.parsed("oob",          String.valueOf(oob)))
                .resolver(Placeholder.parsed("arr_rate",     String.format("%.1f", arrRate)))
                .resolver(Placeholder.parsed("arr_verdict",  arrVerdict))
                .resolver(Placeholder.parsed("bounce_pct",   String.format("%.1f", bounceAvg)))
                .resolver(Placeholder.parsed("bounce_verdict", bounceVerdict))
                .resolver(Placeholder.parsed("bounce_ticks", String.valueOf(bounceTix)))
                .resolver(Placeholder.parsed("total_ticks",  String.valueOf(ticks)))
                .resolver(Placeholder.parsed("btix_pct",     String.format("%.1f", bounceTixPct)))
                .resolver(Placeholder.parsed("align_avg",    String.format("%.3f", alignAvg)))
                .resolver(Placeholder.parsed("align_verdict",alignVerdict))
                .resolver(Placeholder.parsed("hist_count",   String.valueOf(intervalHistory.size())))
                .resolver(Placeholder.parsed("interval_t",   String.valueOf(INTERVAL_TICKS)))
                .resolver(Placeholder.parsed("trend",        trendBlock))
                .build();

        Component report = mm.deserialize("""
                <gray>══════════════════════════════════════════════════════
                <white> MODEL SESSION REPORT — SHARED BRAIN
                <gray>══════════════════════════════════════════════════════
                <gray> Runtime        : <white><ticks> ticks <gray>(<seconds>s / <hours>h)
                <gray> Dragons spawned: <white><spawned>
                <gray> Total reward   : <<reward_color>><reward_total><gray>  |  Per-tick: <<reward_color>><per_tick>
                <gray> Peak interval  : <white><peak>  <gray>| Worst: <white><worst>
                <gray>──────────────────────────────────────────────────────
                <gray> TASK COMPLETION
                <gray> Arrivals   : <white><arrivals>  <gray>Timeouts: <white><timeouts>  <gray>OOB: <white><oob>
                <gray> Success    : <white><arr_rate>%%  <arr_verdict>
                <gray>──────────────────────────────────────────────────────
                <gray> BEHAVIOR HEALTH
                <gray> Bounce %%   : <white><bounce_pct>%%  <bounce_verdict>
                <gray> Bounce ticks: <white><bounce_ticks> <gray>/ <white><total_ticks> <gray>(<btix_pct>%%)
                <gray> Avg alignment: <white><align_avg>  <align_verdict>
                <gray>──────────────────────────────────────────────────────
                <gray> LEARNING TREND  (last <hist_count> intervals × <interval_t> ticks)
                <trend>
                <gray>══════════════════════════════════════════════════════
                """, tagResolver);

        logger.info(report);
    }

    @NotNull
    private String buildTrendBlock() {
        if (intervalHistory.size() < 4) {
            return " <gray>(insufficient history — need at least 4 intervals)";
        }

        double[] h = intervalHistory.stream().mapToDouble(Double::doubleValue).toArray();
        int n = h.length;
        int q = Math.max(1, n / 4);

        double earlyAvg  = 0.0;
        double recentAvg = 0.0;
        for (int i = 0;       i < q;     i++) earlyAvg  += h[i];
        for (int i = n - q;   i < n;     i++) recentAvg += h[i];
        earlyAvg  /= q;
        recentAvg /= q;

        double delta = recentAvg - earlyAvg;

        String trendTag;
        String diagnosis;
        if (delta > 2.0) {
            trendTag  = "<green>✔ IMPROVING  (+%.2f)".formatted(delta);
            diagnosis = "<green> Model is learning the task correctly.";
        } else if (delta > 0.0) {
            trendTag  = "<yellow>~ SLOW IMPROVEMENT (+%.2f)".formatted(delta);
            diagnosis = "<yellow> Learning but slowly. Check buffer size and LR.";
        } else if (delta > -2.0) {
            trendTag  = "<yellow>~ PLATEAU (%.2f)".formatted(delta);
            diagnosis = "<yellow> Possible convergence or local optimum. Inspect bounce %.";
        } else {
            trendTag  = "<red>✘ REGRESSING (%.2f)".formatted(delta);
            diagnosis = "<red> Reward degrading. Likely buffer contamination or inverted signal.\n <red>→ Consider: reduce remorseTraceBufferCapacity, lower remorseMinimumSimilarityThreshold.";
        }

        return (" <gray>Early avg   : <white>%.4f\n <gray>Recent avg  : <white>%.4f\n <gray>Δ trend     : %s\n%s")
                .formatted(earlyAvg, recentAvg, trendTag, diagnosis);
    }

    @NotNull
    @Contract(pure = true)
    private static String verdict(double val, double good, double ok, @NotNull String g, @NotNull String w, @NotNull String b) {
        if (val >= good) return "<green>" + g;
        if (val >= ok)   return "<yellow>" + w;
        return "<red>" + b;
    }

    @NotNull
    @Contract(pure = true)
    private static String verdictInv(double val, double good, double bad, @NotNull String g, @NotNull String w, @NotNull String b) {
        if (val <= good) return "<green>" + g;
        if (val <= bad)  return "<yellow>" + w;
        return "<red>" + b;
    }
}