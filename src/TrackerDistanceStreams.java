import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import nz.sodium.Cell;
import nz.sodium.Stream;
import nz.sodium.Tuple2;

/** Builds independent Sodium distance state for each tracker's filtered event stream. */
public final class TrackerDistanceStreams {
    private final Map<String, Cell<Optional<Long>>> distances;

    public TrackerDistanceStreams(Stream<ReceivedGpsEvent> filteredEvents) {
        Objects.requireNonNull(filteredEvents, "filteredEvents");

        Map<String, Cell<Optional<Long>>> mutableDistances = new LinkedHashMap<>();
        for (String tracker : SimplifiedGpsStreams.trackerNames()) {
            Stream<DistanceSample> trackerSamples = filteredEvents
                    .filter(event -> tracker.equals(event.getTracker()))
                    .map(DistanceSample::from);
            Stream<DistanceWindow> windowUpdates = trackerSamples.collect(
                    DistanceWindow.empty(), (sample, previous) -> {
                        DistanceWindow next = previous.append(sample);
                        return new Tuple2<>(next, next);
                    });
            Cell<DistanceWindow> window = windowUpdates.hold(DistanceWindow.empty());
            mutableDistances.put(tracker, window.map(DistanceWindow::distanceMeters));
        }
        this.distances = Collections.unmodifiableMap(mutableDistances);
    }

    public Map<String, Cell<Optional<Long>>> getDistances() {
        return distances;
    }

    public Cell<Optional<Long>> getDistanceMeters(String tracker) {
        Cell<Optional<Long>> distance = distances.get(tracker);
        if (distance == null) {
            throw new IllegalArgumentException("Unknown tracker slot: " + tracker);
        }
        return distance;
    }
}
