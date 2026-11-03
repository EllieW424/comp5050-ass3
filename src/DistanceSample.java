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
        return create(event.name, event.latitude, event.longitude,
                event.altitude, receivedAtNanos);
    }

    public static DistanceSample from(ReceivedGpsEvent event) {
        Objects.requireNonNull(event, "event");
        return create(event.getTracker(), event.getLatitude(), event.getLongitude(),
                event.getAltitudeFeet(), event.getReceivedAtNanos());
    }

    private static DistanceSample create(String tracker, double latitude, double longitude,
                                         double altitudeFeet, long receivedAtNanos) {
        GpsValidation.requireTrackerName(tracker);
        GpsValidation.requireCoordinates(latitude, longitude);
        if (!Double.isFinite(altitudeFeet)) {
            throw new IllegalArgumentException("Altitude in feet must be finite");
        }

        double altitudeMeters = altitudeFeet * METERS_PER_FOOT;
        if (!Double.isFinite(altitudeMeters)) {
            throw new IllegalArgumentException("Converted altitude in meters must be finite");
        }
        return new DistanceSample(tracker, latitude, longitude, altitudeMeters,
                receivedAtNanos);
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
