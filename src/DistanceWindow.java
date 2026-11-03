import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Immutable, timestamp-ordered samples retained for one tracker's rolling distance. */
public final class DistanceWindow {
    public static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(5L);

    private final List<DistanceSample> samples;

    private DistanceWindow(List<DistanceSample> samples) {
        this.samples = Collections.unmodifiableList(samples);
    }

    public static DistanceWindow empty() {
        return new DistanceWindow(Collections.emptyList());
    }

    public DistanceWindow append(DistanceSample sample) {
        Objects.requireNonNull(sample, "sample");

        List<DistanceSample> ordered = new ArrayList<>(samples);
        int insertionPoint = upperBound(sample.getReceivedAtNanos());
        ordered.add(insertionPoint, sample);

        long newestTimestamp = ordered.get(ordered.size() - 1).getReceivedAtNanos();
        long cutoff = newestTimestamp < Long.MIN_VALUE + WINDOW_NANOS
                ? Long.MIN_VALUE : newestTimestamp - WINDOW_NANOS;
        int firstRetained = 0;
        while (firstRetained < ordered.size()
                && ordered.get(firstRetained).getReceivedAtNanos() < cutoff) {
            firstRetained++;
        }
        if (firstRetained > 0) {
            ordered = new ArrayList<>(ordered.subList(firstRetained, ordered.size()));
        }
        return new DistanceWindow(ordered);
    }

    public int size() {
        return samples.size();
    }

    public Optional<Long> distanceMeters() {
        if (samples.isEmpty()) {
            return Optional.empty();
        }

        double totalMeters = 0.0;
        for (int index = 1; index < samples.size(); index++) {
            totalMeters += DistanceCalculator.between(
                    samples.get(index - 1), samples.get(index));
        }
        return Optional.of((long) Math.ceil(totalMeters));
    }

    private int upperBound(long timestampNanos) {
        int low = 0;
        int high = samples.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (samples.get(middle).getReceivedAtNanos() <= timestampNanos) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }
}
