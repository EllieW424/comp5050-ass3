import java.util.Objects;

public final class SimplifiedGps {
    private final String tracker;
    private final double latitude;
    private final double longitude;

    public SimplifiedGps(String tracker, double latitude, double longitude) {
        this.tracker = GpsValidation.requireTrackerName(tracker);
        GpsValidation.requireCoordinates(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static SimplifiedGps from(GpsEvent event) {
        Objects.requireNonNull(event, "event");
        return new SimplifiedGps(event.name, event.latitude, event.longitude);
    }

    public static SimplifiedGps from(ReceivedGpsEvent event) {
        Objects.requireNonNull(event, "event");
        return new SimplifiedGps(event.getTracker(), event.getLatitude(), event.getLongitude());
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
}
