import java.util.Objects;

public final class FormattedEvent {
    private final String tracker;
    private final double latitude;
    private final double longitude;
    private final double altitudeFeet;

    private FormattedEvent(String tracker, double latitude, double longitude,
                           double altitudeFeet) {
        this.tracker = tracker;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeFeet = altitudeFeet;
    }

    public static FormattedEvent from(GpsEvent event) {
        Objects.requireNonNull(event, "event");
        String tracker = GpsValidation.requireTrackerName(event.name);
        GpsValidation.requireCoordinates(event.latitude, event.longitude);
        if (!Double.isFinite(event.altitude)) {
            throw new IllegalArgumentException("Altitude in feet must be finite");
        }
        return new FormattedEvent(tracker, event.latitude, event.longitude, event.altitude);
    }

    public static FormattedEvent from(ReceivedGpsEvent event) {
        Objects.requireNonNull(event, "event");
        return from(event.toGpsEvent());
    }

    public String format() {
        return tracker + "," + Double.toString(latitude) + ","
                + Double.toString(longitude) + "," + Double.toString(altitudeFeet);
    }

    @Override
    public String toString() {
        return format();
    }
}
