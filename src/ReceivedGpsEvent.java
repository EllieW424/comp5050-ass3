import java.util.Objects;

/** Immutable event snapshot with the local time at which the source stream fired. */
public final class ReceivedGpsEvent {
    private final String tracker;
    private final double latitude;
    private final double longitude;
    private final double altitudeFeet;
    private final long receivedAtNanos;

    private ReceivedGpsEvent(String tracker, double latitude, double longitude,
                             double altitudeFeet, long receivedAtNanos) {
        this.tracker = tracker;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeFeet = altitudeFeet;
        this.receivedAtNanos = receivedAtNanos;
    }

    public static ReceivedGpsEvent from(GpsEvent event, long receivedAtNanos) {
        Objects.requireNonNull(event, "event");
        String tracker = GpsValidation.requireTrackerName(event.name);
        GpsValidation.requireCoordinates(event.latitude, event.longitude);
        if (!Double.isFinite(event.altitude)) {
            throw new IllegalArgumentException("Altitude in feet must be finite");
        }
        return new ReceivedGpsEvent(tracker, event.latitude, event.longitude,
                event.altitude, receivedAtNanos);
    }

    public String getTracker() {
        return tracker;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitudeFeet() {
        return altitudeFeet;
    }

    public long getReceivedAtNanos() {
        return receivedAtNanos;
    }

    public GpsEvent toGpsEvent() {
        return new GpsEvent(tracker, latitude, longitude, altitudeFeet);
    }
}
