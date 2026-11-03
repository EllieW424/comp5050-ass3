import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import nz.sodium.Cell;
import nz.sodium.Stream;

/** Holds the latest altitude-free position for each course tracker slot. */
public final class SimplifiedGpsStreams {
    public static final int TRACKER_COUNT = 10;

    private static final List<String> TRACKER_NAMES = createTrackerNames();

    private final Map<String, Cell<Optional<SimplifiedGps>>> positions;

    public SimplifiedGpsStreams(Stream<ReceivedGpsEvent> events) {
        Objects.requireNonNull(events, "events");

        Stream<SimplifiedGps> simplified = events.map(SimplifiedGps::from);
        Map<String, Cell<Optional<SimplifiedGps>>> mutablePositions = new LinkedHashMap<>();
        for (String tracker : TRACKER_NAMES) {
            Cell<Optional<SimplifiedGps>> position = simplified
                    .filter(gps -> tracker.equals(gps.getTracker()))
                    .map(Optional::of)
                    .hold(Optional.empty());
            mutablePositions.put(tracker, position);
        }
        this.positions = Collections.unmodifiableMap(mutablePositions);
    }

    public List<String> getTrackerNames() {
        return TRACKER_NAMES;
    }

    public Map<String, Cell<Optional<SimplifiedGps>>> getPositions() {
        return positions;
    }

    public Cell<Optional<SimplifiedGps>> getPosition(String tracker) {
        Cell<Optional<SimplifiedGps>> position = positions.get(tracker);
        if (position == null) {
            throw new IllegalArgumentException("Unknown tracker slot: " + tracker);
        }
        return position;
    }

    private static List<String> createTrackerNames() {
        List<String> names = new ArrayList<>(TRACKER_COUNT);
        for (int index = 0; index < TRACKER_COUNT; index++) {
            names.add("Tracker" + index);
        }
        return Collections.unmodifiableList(names);
    }
}
