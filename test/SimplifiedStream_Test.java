import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.Test;

import nz.sodium.StreamSink;

public class SimplifiedStream_Test {
    @Test
    public void zeroEventsLeaveAllTenSlotsEmpty() {
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(new StreamSink<>());

        assertEquals(10, positions.getTrackerNames().size());
        assertEquals(10, positions.getPositions().size());
        for (String tracker : positions.getTrackerNames()) {
            assertFalse(positions.getPosition(tracker).sample().isPresent());
        }
    }

    @Test
    public void oneTrackerEventUpdatesOnlyItsSlot() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);

        events.send(received("Tracker4", -12.3456789, -77.2500001, 600.0));

        assertPosition(positions.getPosition("Tracker4").sample(),
                "Tracker4", -12.3456789, -77.2500001);
        assertOnlyTrackersUpdated(positions, "Tracker4");
    }

    @Test
    public void twoTrackerEventsRemainIndependentAndLaterUpdateReplacesOnlyOne() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);

        events.send(received("Tracker1", -10.25, -20.5, 300.0));
        events.send(received("Tracker7", 40.5, 80.25, 900.0));
        events.send(received("Tracker1", -11.75, -21.5, 1200.0));

        assertPosition(positions.getPosition("Tracker1").sample(),
                "Tracker1", -11.75, -21.5);
        assertPosition(positions.getPosition("Tracker7").sample(),
                "Tracker7", 40.5, 80.25);
        assertOnlyTrackersUpdated(positions, "Tracker1", "Tracker7");
    }

    @Test
    public void tenTrackerEventsPopulateAllSlotsWithExactCoordinates() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);

        for (int index = 0; index < SimplifiedGpsStreams.TRACKER_COUNT; index++) {
            double latitude = -45.123456789 + index * 7.125;
            double longitude = -120.987654321 + index * 11.375;
            events.send(received("Tracker" + index, latitude, longitude, 100.0 + index));
        }

        for (int index = 0; index < SimplifiedGpsStreams.TRACKER_COUNT; index++) {
            double latitude = -45.123456789 + index * 7.125;
            double longitude = -120.987654321 + index * 11.375;
            assertPosition(positions.getPosition("Tracker" + index).sample(),
                    "Tracker" + index, latitude, longitude);
        }
    }

    @Test
    public void simplifiedPositionsPreserveSignAndPrecisionWithoutAltitudeOrFeet() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);

        events.send(received("Tracker2", -0.000000123456, -179.999999876543, 5280.0));

        SimplifiedGps point = positions.getPosition("Tracker2").sample().get();
        assertEquals(-0.000000123456, point.getLatitude(), 0.0);
        assertEquals(-179.999999876543, point.getLongitude(), 0.0);

        Field[] fields = SimplifiedGps.class.getDeclaredFields();
        assertEquals(3, fields.length);
        for (Field field : fields) {
            String name = field.getName().toLowerCase();
            assertFalse("Unexpected altitude/feet field: " + field.getName(),
                    name.contains("altitude") || name.contains("feet"));
            assertTrue("Unexpected simplified field: " + field.getName(),
                    name.equals("tracker") || name.equals("latitude")
                            || name.equals("longitude"));
        }
    }

    private static void assertOnlyTrackersUpdated(SimplifiedGpsStreams positions,
                                                  String... updatedTrackers) {
        for (String tracker : positions.getTrackerNames()) {
            boolean shouldHaveValue = false;
            for (String updatedTracker : updatedTrackers) {
                if (tracker.equals(updatedTracker)) {
                    shouldHaveValue = true;
                    break;
                }
            }
            assertEquals("Unexpected slot state for " + tracker,
                    shouldHaveValue, positions.getPosition(tracker).sample().isPresent());
        }
    }

    private static void assertPosition(Optional<SimplifiedGps> value, String tracker,
                                       double latitude, double longitude) {
        assertTrue("Expected a position for " + tracker, value.isPresent());
        SimplifiedGps point = value.get();
        assertEquals(tracker, point.getTracker());
        assertEquals(latitude, point.getLatitude(), 0.0);
        assertEquals(longitude, point.getLongitude(), 0.0);
    }

    private static ReceivedGpsEvent received(String tracker, double latitude,
                                             double longitude, double altitudeFeet) {
        return ReceivedGpsEvent.from(
                new GpsEvent(tracker, latitude, longitude, altitudeFeet), 0L);
    }
}
