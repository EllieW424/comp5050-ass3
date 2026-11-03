import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import nz.sodium.Cell;
import nz.sodium.StreamSink;

public class SimplifiedGpsStreams_Test {
    @Test
    public void createsTenInitiallyEmptyTrackerSlots() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);

        assertEquals(10, positions.getPositions().size());
        assertEquals("Tracker0", positions.getTrackerNames().get(0));
        assertEquals("Tracker9", positions.getTrackerNames().get(9));
        for (Cell<Optional<SimplifiedGps>> position : positions.getPositions().values()) {
            assertFalse(position.sample().isPresent());
        }
    }

    @Test
    public void trackerPositionsUpdateIndependentlyAndKeepOnlyTheLatestValue() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);

        events.send(received("Tracker1", -10.25, -20.5, 300.0));
        events.send(received("Tracker7", 40.5, 80.25, 900.0));
        events.send(received("Tracker1", -11.75, -21.5, 1200.0));

        Map<String, Cell<Optional<SimplifiedGps>>> slots = positions.getPositions();
        SimplifiedGps tracker1 = slots.get("Tracker1").sample().get();
        SimplifiedGps tracker7 = slots.get("Tracker7").sample().get();

        assertEquals(-11.75, tracker1.getLatitude(), 0.0);
        assertEquals(-21.5, tracker1.getLongitude(), 0.0);
        assertEquals(40.5, tracker7.getLatitude(), 0.0);
        assertEquals(80.25, tracker7.getLongitude(), 0.0);
        assertTrue(slots.get("Tracker0").sample().isEmpty());
    }

    private static ReceivedGpsEvent received(String tracker, double latitude,
                                             double longitude, double altitudeFeet) {
        return ReceivedGpsEvent.from(
                new GpsEvent(tracker, latitude, longitude, altitudeFeet), 0L);
    }
}
