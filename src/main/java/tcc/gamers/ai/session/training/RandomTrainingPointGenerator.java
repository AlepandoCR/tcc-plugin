package tcc.gamers.ai.session.training;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;


public class RandomTrainingPointGenerator implements Supplier<List<Location>> {

    private final World world;
    private final Location center;
    private final double minRadius;
    private final double maxRadius;
    private final double verticalRange;
    private final double minY;
    private final double maxY;
    private final int batchSize;

    /**
     * The current working batch. Consumed front-to-back via currentIndex.
     * Replaced entirely when currentIndex reaches batchSize.
     */
    private List<Location> batch;
    private int currentIndex = 0;

    /** Total number of batches generated since construction. For diagnostics. */
    private int batchesGenerated = 0;

    /**
     * @param world         The world all generated points belong to.
     * @param center        The anchor point around which targets are distributed.
     * @param minRadius     Minimum horizontal distance from center (blocks). Default 20.
     * @param maxRadius     Maximum horizontal distance from center (blocks). Default 80.
     * @param verticalRange Maximum Y deviation above/below center.y (blocks). Default 15.
     * @param minY          Hard floor — no point will be generated below this Y. Default 5.
     * @param maxY          Hard ceiling — no point will be generated above this Y. Default 250.
     * @param batchSize     How many points to pre-generate per batch. Default 32.
     * Higher values = more diversity per batch, slightly more memory.
     */
    public RandomTrainingPointGenerator(World world, Location center, double minRadius, double maxRadius,
    double verticalRange, double minY, double maxY, int batchSize) {
        if (minRadius < 0) throw new IllegalArgumentException("minRadius must be >= 0, got " + minRadius);
        if (maxRadius <= minRadius) throw new IllegalArgumentException("maxRadius must be > minRadius, got " + maxRadius + " <= " + minRadius);
        if (verticalRange < 0) throw new IllegalArgumentException("verticalRange must be >= 0, got " + verticalRange);
        if (minY >= maxY) throw new IllegalArgumentException("minY must be < maxY, got " + minY + " >= " + maxY);
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be >= 1, got " + batchSize);

        this.world = world;
        this.center = center;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.verticalRange = verticalRange;
        this.minY = minY;
        this.maxY = maxY;
        this.batchSize = batchSize;

        this.batch = generateBatch();
    }

    /**
     * Overloaded constructor to support the default parameters from the original Kotlin code.
     */
    public RandomTrainingPointGenerator(World world, Location center) {
        this(world, center, 40.0, 70.0, 15.0, 5.0, 250.0, 32);
    }

    @Override
    public List<Location> get() {
        if (currentIndex >= batchSize) {
            batch = generateBatch();
            currentIndex = 0;
        }
        currentIndex++;
        return batch;
    }

    public Location next() {
        if (currentIndex >= batchSize) {
            batch = generateBatch();
            currentIndex = 0;
        }
        return batch.get(currentIndex++);
    }

    /** How many points remain in the current batch before it regenerates. */
    public int getRemaining() {
        return batchSize - currentIndex;
    }

    /** Total number of batches generated since construction. For diagnostics. */
    public int getBatchesGenerated() {
        return batchesGenerated;
    }

    /**
     * Generates a new batch of batchSize locations using stratified angular
     * sampling to ensure uniform horizontal coverage.
     */
    @NotNull
    private List<Location> generateBatch() {
        batchesGenerated++;

        double sectorSize = (2.0 * Math.PI) / batchSize;
        double cx = center.getX();
        double cy = center.getY();
        double cz = center.getZ();

        List<Location> newBatch = new ArrayList<>(batchSize);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < batchSize; i++) {
        double sectorStart = i * sectorSize;
        double angle = sectorStart + random.nextDouble(0.0, sectorSize);

        // Random radius in [minRadius, maxRadius]
        double radius = random.nextDouble(minRadius, maxRadius);

        double x = cx + radius * Math.cos(angle);
        double z = cz + radius * Math.sin(angle);

        // Random Y offset, clamped to world bounds
        double yOffset = random.nextDouble(-verticalRange, verticalRange);
        double y = cy + yOffset;

        y = Math.max(minY, Math.min(maxY, y));

        newBatch.add(new Location(world, x, y, z));
    }

        return newBatch;
    }
}