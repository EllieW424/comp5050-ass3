import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Test;

public class GpsModel_Test {
    @Test
    public void acceptsLatitudeEndpointsAndZero() {
        assertEquals(-90.0, new SimplifiedGps("Tracker0", -90.0, 0.0).getLatitude(), 0.0);
        assertEquals(0.0, new SimplifiedGps("Tracker0", 0.0, 0.0).getLatitude(), 0.0);
        assertEquals(90.0, new SimplifiedGps("Tracker0", 90.0, 0.0).getLatitude(), 0.0);
    }

    @Test
    public void rejectsLatitudeOutsideRangeAndNonFiniteValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", -90.000001, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", 90.000001, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", Double.POSITIVE_INFINITY, 0.0));
    }

    @Test
    public void acceptsLongitudeEndpointsAndZero() {
        assertEquals(-180.0, new SimplifiedGps("Tracker0", 0.0, -180.0).getLongitude(), 0.0);
        assertEquals(0.0, new SimplifiedGps("Tracker0", 0.0, 0.0).getLongitude(), 0.0);
        assertEquals(180.0, new SimplifiedGps("Tracker0", 0.0, 180.0).getLongitude(), 0.0);
    }

    @Test
    public void rejectsLongitudeOutsideRangeAndNonFiniteValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", 0.0, -180.000001));
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", 0.0, 180.000001));
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", 0.0, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps("Tracker0", 0.0, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void preservesNegativeCoordinatesAndSnapshotsSimplifiedFields() {
        GpsEvent event = new GpsEvent("Tracker2", -12.5, -77.25, 600.0);
        SimplifiedGps point = SimplifiedGps.from(event);
        event.name = "Tracker9";
        event.latitude = 1.0;
        event.longitude = 2.0;

        assertEquals("Tracker2", point.getTracker());
        assertEquals(-12.5, point.getLatitude(), 0.0);
        assertEquals(-77.25, point.getLongitude(), 0.0);
    }

    @Test
    public void simplifiedModelDoesNotStoreAltitude() {
        for (Field field : SimplifiedGps.class.getDeclaredFields()) {
            assertFalse("Unexpected altitude field: " + field.getName(),
                    field.getName().toLowerCase().contains("altitude"));
        }
    }

    @Test
    public void convertsZeroAndOneFootToMetersAndRetainsArrivalTime() {
        DistanceSample zero = DistanceSample.from(
                new GpsEvent("Tracker0", 0.0, 0.0, 0.0), 0L);
        DistanceSample oneFoot = DistanceSample.from(
                new GpsEvent("Tracker1", -1.0, -2.0, 1.0), -42L);

        assertEquals(0.0, zero.getAltitudeMeters(), 0.0);
        assertEquals(0.3048, oneFoot.getAltitudeMeters(), 0.0);
        assertEquals("Tracker1", oneFoot.getTracker());
        assertEquals(-1.0, oneFoot.getLatitude(), 0.0);
        assertEquals(-2.0, oneFoot.getLongitude(), 0.0);
        assertEquals(-42L, oneFoot.getReceivedAtNanos());
    }

    @Test
    public void rejectsNonFiniteAltitudeAtDistanceBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> DistanceSample.from(
                        new GpsEvent("Tracker0", 0.0, 0.0, Double.NaN), 0L));
        assertThrows(IllegalArgumentException.class,
                () -> DistanceSample.from(
                        new GpsEvent("Tracker0", 0.0, 0.0, Double.POSITIVE_INFINITY), 0L));
    }

    @Test
    public void formatsExactlyFourSourceOrderedCommaSeparatedFields() {
        GpsEvent event = new GpsEvent("Tracker3", -12.5, -77.25, 1.0);
        FormattedEvent formatted = FormattedEvent.from(event);
        event.latitude = 0.0;
        event.altitude = 0.0;

        assertEquals("Tracker3,-12.5,-77.25,1.0", formatted.format());
        assertEquals(formatted.format(), formatted.toString());
        assertEquals(4, formatted.format().split(",", -1).length);
    }

    @Test
    public void rejectsInvalidBoundsAndIncludesBothEndpoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new Bounds(1.0, 0.0, -1.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new Bounds(-1.0, 1.0, 2.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new Bounds(-90.1, 90.0, -180.0, 180.0));

        Bounds bounds = new Bounds(-10.0, 10.0, -20.0, 20.0);
        assertTrue(bounds.contains(-10.0, -20.0));
        assertTrue(bounds.contains(10.0, 20.0));
        assertTrue(bounds.contains(0.0, 0.0));
        assertFalse(bounds.contains(10.0001, 0.0));
        assertFalse(bounds.contains(0.0, 20.0001));
        assertFalse(bounds.contains(Double.NaN, 0.0));
    }

    @Test
    public void rejectsTrackerNamesThatCannotBeRepresentedAsOneField() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimplifiedGps(" ", 0.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> FormattedEvent.from(new GpsEvent("Tracker,4", 0.0, 0.0, 0.0)));
        assertNotNull(new SimplifiedGps("Tracker4", 0.0, 0.0));
    }
}
