import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import nz.sodium.Stream;
import nz.sodium.StreamSink;

public class GpsServiceAdapter_Test {
    @Test
    public void mergesEveryInputStreamAndCapturesArrivalTime() {
        StreamSink<GpsEvent> first = new StreamSink<>();
        StreamSink<GpsEvent> second = new StreamSink<>();
        StreamSink<GpsEvent> third = new StreamSink<>();
        @SuppressWarnings("unchecked")
        Stream<GpsEvent>[] streams = new Stream[] { first, second, third };
        AtomicLong clock = new AtomicLong(100L);
        GpsServiceAdapter adapter = new GpsServiceAdapter(streams, clock::getAndIncrement);
        List<ReceivedGpsEvent> received = new ArrayList<>();
        adapter.events().listen(received::add);

        first.send(new GpsEvent("Tracker0", -1.0, -2.0, 10.0));
        second.send(new GpsEvent("Tracker1", 1.0, 2.0, 20.0));
        third.send(new GpsEvent("Tracker2", 3.0, 4.0, 30.0));

        assertEquals(3, received.size());
        assertEquals("Tracker0", received.get(0).getTracker());
        assertEquals("Tracker1", received.get(1).getTracker());
        assertEquals("Tracker2", received.get(2).getTracker());
        assertEquals(100L, received.get(0).getReceivedAtNanos());
        assertEquals(101L, received.get(1).getReceivedAtNanos());
        assertEquals(102L, received.get(2).getReceivedAtNanos());
    }

    @Test
    public void snapshotsMutableSourceEventAndRetainsRawAltitude() {
        StreamSink<GpsEvent> source = new StreamSink<>();
        @SuppressWarnings("unchecked")
        Stream<GpsEvent>[] streams = new Stream[] { source };
        GpsServiceAdapter adapter = new GpsServiceAdapter(streams, () -> 77L);
        List<ReceivedGpsEvent> received = new ArrayList<>();
        adapter.events().listen(received::add);
        GpsEvent sourceEvent = new GpsEvent("Tracker4", -12.5, -77.25, 600.0);

        source.send(sourceEvent);
        sourceEvent.name = "changed";
        sourceEvent.latitude = 0.0;
        sourceEvent.altitude = 0.0;

        ReceivedGpsEvent snapshot = received.get(0);
        assertEquals("Tracker4", snapshot.getTracker());
        assertEquals(-12.5, snapshot.getLatitude(), 0.0);
        assertEquals(-77.25, snapshot.getLongitude(), 0.0);
        assertEquals(600.0, snapshot.getAltitudeFeet(), 0.0);
        assertEquals(77L, snapshot.getReceivedAtNanos());
        assertEquals("Tracker4", snapshot.toGpsEvent().name);
        assertEquals(600.0, snapshot.toGpsEvent().altitude, 0.0);
    }

    @Test
    public void handlesEmptyInputWithoutCreatingAServiceInTestMode() {
        @SuppressWarnings("unchecked")
        Stream<GpsEvent>[] streams = new Stream[0];
        GpsServiceAdapter adapter = new GpsServiceAdapter(streams, System::nanoTime);
        assertNull(adapter.getService());
        assertNotNull(adapter.events());
    }
}
