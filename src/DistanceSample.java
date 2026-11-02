import java.util.Objects;

public final class DistanceSample {
    public static final double METERS_PER_FOOT = 0.3048;

    private final String tracker;
    private final double latitude;
    private final double longitude;
    private final double altitudeMeters;
    private final long receivedAtNanos;

    private DistanceSample(String tracker, double latitude, double longitude,
                           double altitudeMeters, long receivedAtNanos) {
        this.tracker = tracker;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.receivedAtNanos = receivedAtNanos;
    }

    public static DistanceSample from(GpsEvent event, long receivedAtNanos) {
        Objects.requireNonNull(event, "event");
        String tracker = GpsValidation.requireTrackerName(event.name);
        GpsValidation.requireCoordinates(event.latitude, event.longitude);
        if (!Double.isFinite(event.altitude)) {
            throw new IllegalArgumentException("Altitude in feet must be finite");
        }

        double altitudeMeters = event.altitude * METERS_PER_FOOT;
        if (!Double.isFinite(altitudeMeters)) {
            throw new IllegalArgumentException("Converted altitude in meters must be finite");
        }
        return new DistanceSample(tracker, event.latitude, event.longitude,
                altitudeMeters, receivedAtNanos);
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

    public double getAltitudeMeters() {
        return altitudeMeters;
    }

    public long getReceivedAtNanos() {
        return receivedAtNanos;
    }
}
