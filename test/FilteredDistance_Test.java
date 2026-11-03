import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import nz.sodium.StreamSink;

public class FilteredDistance_Test {
    @Test
    public void distanceIsEmptyBeforeDataAndZeroUntilTwoPositionsAreKnown() {
        Fixture fixture = new Fixture();
        assertFalse(fixture.distance("Tracker0").isPresent());

        fixture.send("Tracker0", 0.0, 0.0, 0.0, 0L);
        assertEquals(Optional.of(0L), fixture.distance("Tracker0"));
        fixture.send("Tracker0", 0.0, 0.0, 0.0, 1L);
        assertEquals(Optional.of(0L), fixture.distance("Tracker0"));
    }

    @Test
    public void distanceWindowIncludesTheFiveMinuteBoundaryAndExpiresOlderSamples() {
        Fixture fixture = new Fixture();
        ReceivedGpsEvent first = fixture.send("Tracker0", 0.0, 0.0, 0.0, 0L);
        ReceivedGpsEvent boundary = fixture.send("Tracker0", 0.0, 1.0, 0.0,
                DistanceWindow.WINDOW_NANOS);

        assertEquals(Optional.of(ceilDistance(first, boundary)),
                fixture.distance("Tracker0"));

        fixture.send("Tracker0", 0.0, 1.0, 0.0,
                DistanceWindow.WINDOW_NANOS + 1L);
        assertEquals(Optional.of(0L), fixture.distance("Tracker0"));
    }

    @Test
    public void trackersAccumulateDistanceIndependently() {
        Fixture fixture = new Fixture();
        ReceivedGpsEvent trackerZeroStart = fixture.send("Tracker0", 0.0, 0.0,
                0.0, 0L);
        ReceivedGpsEvent trackerOneStart = fixture.send("Tracker1", 10.0, 10.0,
                0.0, 1L);
        ReceivedGpsEvent trackerZeroFinish = fixture.send("Tracker0", 0.0, 1.0,
                0.0, 2L);

        assertEquals(Optional.of(ceilDistance(trackerZeroStart, trackerZeroFinish)),
                fixture.distance("Tracker0"));
        assertEquals(Optional.of(0L), fixture.distance("Tracker1"));

        ReceivedGpsEvent trackerOneFinish = fixture.send("Tracker1", 10.0, 11.0,
                0.0, 3L);
        assertEquals(Optional.of(ceilDistance(trackerOneStart, trackerOneFinish)),
                fixture.distance("Tracker1"));
        assertEquals(Optional.of(ceilDistance(trackerZeroStart, trackerZeroFinish)),
                fixture.distance("Tracker0"));
    }

    @Test
    public void latitudeFilterExcludesOutsidePointsFromDistance() {
        Fixture fixture = new Fixture();
        fixture.setRange(GpsFilterModel.Axis.LATITUDE, -1.0, 1.0);
        ReceivedGpsEvent start = fixture.send("Tracker0", 0.0, 0.0, 0.0, 0L);
        fixture.send("Tracker0", 10.0, 0.0, 0.0, 1L);
        ReceivedGpsEvent finish = fixture.send("Tracker0", 0.0, 1.0, 0.0, 2L);

        assertEquals(Optional.of(ceilDistance(start, finish)), fixture.distance("Tracker0"));
    }

    @Test
    public void longitudeFilterExcludesOutsidePointsFromDistance() {
        Fixture fixture = new Fixture();
        fixture.setRange(GpsFilterModel.Axis.LONGITUDE, -1.0, 1.0);
        ReceivedGpsEvent start = fixture.send("Tracker0", 0.0, 0.0, 0.0, 0L);
        fixture.send("Tracker0", 10.0, 10.0, 0.0, 1L);
        ReceivedGpsEvent finish = fixture.send("Tracker0", 1.0, 0.0, 0.0, 2L);

        assertEquals(Optional.of(ceilDistance(start, finish)), fixture.distance("Tracker0"));
    }

    @Test
    public void combinedFilterRequiresBothLatitudeAndLongitudeToMatch() {
        Fixture fixture = new Fixture();
        fixture.setRange(GpsFilterModel.Axis.LATITUDE, 10.0, 20.0);
        fixture.setRange(GpsFilterModel.Axis.LONGITUDE, 30.0, 40.0);
        ReceivedGpsEvent start = fixture.send("Tracker0", 15.0, 35.0, 0.0, 0L);
        fixture.send("Tracker0", 25.0, 35.0, 0.0, 1L);
        fixture.send("Tracker0", 15.0, 45.0, 0.0, 2L);
        ReceivedGpsEvent finish = fixture.send("Tracker0", 16.0, 35.0, 0.0, 3L);

        assertEquals(Optional.of(ceilDistance(start, finish)), fixture.distance("Tracker0"));
    }

    @Test
    public void changingBoundsAffectsFutureEventsWithoutRefilteringHistory() {
        Fixture tightened = new Fixture();
        tightened.setRange(GpsFilterModel.Axis.LATITUDE, 0.0, 10.0);
        ReceivedGpsEvent oldAccepted = tightened.send("Tracker0", 0.0, 0.0,
                0.0, 0L);
        ReceivedGpsEvent retainedAccepted = tightened.send("Tracker0", 10.0, 0.0,
                0.0, 1L);
        tightened.setRange(GpsFilterModel.Axis.LATITUDE, 9.0, 10.0);
        ReceivedGpsEvent afterTightening = tightened.send("Tracker0", 10.0, 1.0,
                0.0, 2L);

        long expectedAfterTightening = ceilRouteDistance(
                oldAccepted, retainedAccepted, afterTightening);
        assertEquals(Optional.of(expectedAfterTightening), tightened.distance("Tracker0"));

        Fixture widened = new Fixture();
        widened.setRange(GpsFilterModel.Axis.LATITUDE, 9.0, 10.0);
        ReceivedGpsEvent acceptedBeforeWidening = widened.send("Tracker0", 9.0, 0.0,
                0.0, 0L);
        widened.send("Tracker0", 0.0, 0.0, 0.0, 1L);
        widened.setRange(GpsFilterModel.Axis.LATITUDE, 0.0, 10.0);
        ReceivedGpsEvent acceptedAfterWidening = widened.send("Tracker0", 9.0, 1.0,
                0.0, 2L);

        assertEquals(Optional.of(ceilDistance(
                acceptedBeforeWidening, acceptedAfterWidening)), widened.distance("Tracker0"));
    }

    @Test
    public void outOfOrderEventsAreInsertedIntoTimestampOrder() {
        Fixture fixture = new Fixture();
        ReceivedGpsEvent first = fixture.send("Tracker0", 0.0, 0.0, 0.0, 0L);
        ReceivedGpsEvent last = fixture.send("Tracker0", 0.0, 1.0, 0.0,
                TimeUnit.SECONDS.toNanos(20L));
        ReceivedGpsEvent middle = fixture.send("Tracker0", 0.0, 2.0, 0.0,
                TimeUnit.SECONDS.toNanos(10L));

        assertEquals(Optional.of(ceilRouteDistance(first, middle, last)),
                fixture.distance("Tracker0"));
    }

    private static long ceilDistance(ReceivedGpsEvent first, ReceivedGpsEvent second) {
        return ceilRouteDistance(first, second);
    }

    private static long ceilRouteDistance(ReceivedGpsEvent... route) {
        double totalMeters = 0.0;
        for (int index = 1; index < route.length; index++) {
            totalMeters += DistanceCalculator.between(
                    DistanceSample.from(route[index - 1]), DistanceSample.from(route[index]));
        }
        return (long) Math.ceil(totalMeters);
    }

    private static final class Fixture {
        private final StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        private final StreamSink<GpsFilterModel.Submission> submissions = new StreamSink<>();
        private final GpsFilterModel filter = new GpsFilterModel(events, submissions);
        private final TrackerDistanceStreams distances =
                new TrackerDistanceStreams(filter.filteredEvents());

        private void setRange(GpsFilterModel.Axis axis, double minimum, double maximum) {
            submissions.send(new GpsFilterModel.Submission(axis,
                    Double.toString(minimum), Double.toString(maximum)));
        }

        private ReceivedGpsEvent send(String tracker, double latitude, double longitude,
                                      double altitudeFeet, long timestampNanos) {
            ReceivedGpsEvent event = ReceivedGpsEvent.from(new GpsEvent(
                    tracker, latitude, longitude, altitudeFeet), timestampNanos);
            events.send(event);
            return event;
        }

        private Optional<Long> distance(String tracker) {
            return distances.getDistanceMeters(tracker).sample();
        }
    }
}
