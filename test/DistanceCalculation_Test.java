import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class DistanceCalculation_Test {
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    @Test
    public void emptyWindowHasNoDistanceAndOnePositionHasZeroMeters() {
        DistanceWindow empty = DistanceWindow.empty();
        assertFalse(empty.distanceMeters().isPresent());

        DistanceWindow onePosition = empty.append(sample(0.0, 0.0, 0.0, 0L));
        assertEquals(Optional.of(0L), onePosition.distanceMeters());
    }

    @Test
    public void identicalPositionsHaveZeroDistance() {
        DistanceWindow window = DistanceWindow.empty()
                .append(sample(-12.5, -77.25, 600.0, 0L))
                .append(sample(-12.5, -77.25, 600.0, TimeUnit.SECONDS.toNanos(1L)));

        assertEquals(0.0, DistanceCalculator.between(
                sample(-12.5, -77.25, 600.0, 0L),
                sample(-12.5, -77.25, 600.0, 1L)), 0.0);
        assertEquals(Optional.of(0L), window.distanceMeters());
    }

    @Test
    public void horizontalDistanceUsesTheDocumentedEquirectangularProjection() {
        DistanceSample first = sample(0.0, 0.0, 0.0, 0L);
        DistanceSample second = sample(0.0, 1.0, 0.0, 1L);
        double expected = EARTH_RADIUS_METERS * Math.toRadians(1.0);

        assertEquals(expected, DistanceCalculator.between(first, second), 1e-9);
        assertEquals(Optional.of((long) Math.ceil(expected)),
                DistanceWindow.empty().append(first).append(second).distanceMeters());
    }

    @Test
    public void altitudeOnlyMovementConvertsFeetAndRoundsUp() {
        DistanceSample ground = sample(45.0, -73.0, 0.0, 0L);
        DistanceSample oneFootHigher = sample(45.0, -73.0, 1.0, 1L);

        assertEquals(0.3048, oneFootHigher.getAltitudeMeters(), 1e-12);
        assertEquals(0.3048, DistanceCalculator.between(ground, oneFootHigher), 1e-12);
        assertEquals(Optional.of(1L),
                DistanceWindow.empty().append(ground).append(oneFootHigher).distanceMeters());
    }

    @Test
    public void windowAddsAdjacentSegmentsAcrossThreeOrMorePositions() {
        DistanceSample start = sample(0.0, 0.0, 0.0, 0L);
        DistanceSample turn = sample(0.0, 1.0, 0.0, 1L);
        DistanceSample finish = sample(0.0, 0.0, 0.0, 2L);
        double segmentMeters = DistanceCalculator.between(start, turn);
        DistanceWindow window = DistanceWindow.empty()
                .append(start)
                .append(turn)
                .append(finish);

        assertEquals(Optional.of((long) Math.ceil(segmentMeters * 2.0)),
                window.distanceMeters());
        assertTrue(window.distanceMeters().get() > 0L);
    }

    @Test
    public void longitudeDeltaCrossesTheDateLineByTheShortDirection() {
        DistanceSample first = sample(0.0, 179.5, 0.0, 0L);
        DistanceSample second = sample(0.0, -179.5, 0.0, 1L);
        double oneDegreeAtEquator = EARTH_RADIUS_METERS * Math.toRadians(1.0);

        assertEquals(oneDegreeAtEquator,
                DistanceCalculator.between(first, second), 1e-9);
    }

    private static DistanceSample sample(double latitude, double longitude,
                                         double altitudeFeet, long timestampNanos) {
        return DistanceSample.from(new GpsEvent(
                "Tracker0", latitude, longitude, altitudeFeet), timestampNanos);
    }
}
