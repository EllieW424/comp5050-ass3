import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nz.sodium.StreamSink;

public class GpsFilterModel_Test {
    @Test
    public void latitudeOnlyBoundsLeaveLongitudeUnrestrictedThenCombineWithLongitude() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        StreamSink<GpsFilterModel.Submission> submissions = new StreamSink<>();
        GpsFilterModel model = new GpsFilterModel(events, submissions);
        List<ReceivedGpsEvent> filtered = new ArrayList<>();
        model.filteredEvents().listen(filtered::add);

        submissions.send(submission(GpsFilterModel.Axis.LATITUDE, "10", "20"));
        assertTrue(model.bounds().sample().getLatitudeRange().isPresent());
        assertFalse(model.bounds().sample().getLongitudeRange().isPresent());
        events.send(received("Tracker1", 15.0, 170.0));
        events.send(received("Tracker2", 21.0, 0.0));
        assertEquals(1, filtered.size());
        assertEquals("Tracker1", filtered.get(0).getTracker());

        submissions.send(submission(GpsFilterModel.Axis.LONGITUDE, "-10", "10"));
        events.send(received("Tracker3", 15.0, 0.0));
        events.send(received("Tracker4", 15.0, 11.0));
        events.send(received("Tracker5", 25.0, 0.0));
        assertEquals(2, filtered.size());
        assertEquals("Tracker3", filtered.get(1).getTracker());

        submissions.send(submission(GpsFilterModel.Axis.LATITUDE, "", ""));
        assertFalse(model.bounds().sample().getLatitudeRange().isPresent());
        assertTrue(model.bounds().sample().getLongitudeRange().isPresent());
        events.send(received("Tracker8", -80.0, 0.0));
        events.send(received("Tracker9", 0.0, 11.0));
        assertEquals(3, filtered.size());
        assertEquals("Tracker8", filtered.get(2).getTracker());
    }

    @Test
    public void longitudeOnlyBoundsLeaveLatitudeUnrestricted() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        StreamSink<GpsFilterModel.Submission> submissions = new StreamSink<>();
        GpsFilterModel model = new GpsFilterModel(events, submissions);
        List<ReceivedGpsEvent> filtered = new ArrayList<>();
        model.filteredEvents().listen(filtered::add);

        submissions.send(submission(GpsFilterModel.Axis.LONGITUDE, "-30", "30"));
        assertFalse(model.bounds().sample().getLatitudeRange().isPresent());
        assertTrue(model.bounds().sample().getLongitudeRange().isPresent());
        events.send(received("Tracker6", 85.0, 25.0));
        events.send(received("Tracker7", 0.0, 30.5));

        assertEquals(1, filtered.size());
        assertEquals("Tracker6", filtered.get(0).getTracker());
    }

    @Test
    public void invalidSubmissionKeepsLastValidBoundsAndValidSubmissionClearsError() {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        StreamSink<GpsFilterModel.Submission> submissions = new StreamSink<>();
        GpsFilterModel model = new GpsFilterModel(events, submissions);

        submissions.send(submission(GpsFilterModel.Axis.LATITUDE, "10", "20"));
        submissions.send(submission(GpsFilterModel.Axis.LATITUDE, "north", "20"));

        assertEquals(10.0, model.bounds().sample().getMinLatitude(), 0.0);
        assertTrue(model.error(GpsFilterModel.Axis.LATITUDE).sample()
                .contains("valid numeric limits"));

        submissions.send(submission(GpsFilterModel.Axis.LATITUDE, "91", "100"));

        assertEquals(10.0, model.bounds().sample().getMinLatitude(), 0.0);
        assertEquals(20.0, model.bounds().sample().getMaxLatitude(), 0.0);
        assertTrue(model.error(GpsFilterModel.Axis.LATITUDE).sample().contains("-90 to 90"));

        submissions.send(submission(GpsFilterModel.Axis.LATITUDE, "", "20"));
        assertTrue(model.error(GpsFilterModel.Axis.LATITUDE).sample().contains("both limits"));
        assertEquals(10.0, model.bounds().sample().getMinLatitude(), 0.0);

        submissions.send(submission(GpsFilterModel.Axis.LATITUDE, "", ""));
        assertFalse(model.bounds().sample().getLatitudeRange().isPresent());
        assertEquals("", model.error(GpsFilterModel.Axis.LATITUDE).sample());
    }

    private static GpsFilterModel.Submission submission(GpsFilterModel.Axis axis,
                                                        String minimum, String maximum) {
        return new GpsFilterModel.Submission(axis, minimum, maximum);
    }

    private static ReceivedGpsEvent received(String tracker, double latitude, double longitude) {
        return ReceivedGpsEvent.from(new GpsEvent(tracker, latitude, longitude, 0.0), 0L);
    }
}
