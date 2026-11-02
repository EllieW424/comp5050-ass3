import java.util.Objects;

final class GpsValidation {
    private GpsValidation() {
    }

    static String requireTrackerName(String tracker) {
        Objects.requireNonNull(tracker, "tracker");
        if (tracker.isBlank() || tracker.indexOf(',') >= 0) {
            throw new IllegalArgumentException("Tracker name must be non-blank and must not contain commas");
        }
        return tracker;
    }

    static void requireCoordinates(double latitude, double longitude) {
        if (!isValidLatitude(latitude)) {
            throw new IllegalArgumentException("Latitude must be finite and in [-90, 90]");
        }
        if (!isValidLongitude(longitude)) {
            throw new IllegalArgumentException("Longitude must be finite and in [-180, 180]");
        }
    }

    static boolean isValidLatitude(double latitude) {
        return Double.isFinite(latitude) && latitude >= -90.0 && latitude <= 90.0;
    }

    static boolean isValidLongitude(double longitude) {
        return Double.isFinite(longitude) && longitude >= -180.0 && longitude <= 180.0;
    }
}
