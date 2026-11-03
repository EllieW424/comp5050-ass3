import java.util.Objects;

/** Calculates three-dimensional movement using a local equirectangular projection. */
public final class DistanceCalculator {
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private DistanceCalculator() {
    }

    public static double between(DistanceSample first, DistanceSample second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        double meanLatitudeRadians = Math.toRadians(
                (first.getLatitude() + second.getLatitude()) / 2.0);
        double latitudeDeltaRadians = Math.toRadians(
                second.getLatitude() - first.getLatitude());
        double longitudeDeltaRadians = Math.toRadians(normalizeLongitudeDelta(
                second.getLongitude() - first.getLongitude()));
        double eastMeters = EARTH_RADIUS_METERS * Math.cos(meanLatitudeRadians)
                * longitudeDeltaRadians;
        double northMeters = EARTH_RADIUS_METERS * latitudeDeltaRadians;
        double horizontalMeters = Math.hypot(eastMeters, northMeters);
        double verticalMeters = second.getAltitudeMeters() - first.getAltitudeMeters();
        return Math.hypot(horizontalMeters, verticalMeters);
    }

    private static double normalizeLongitudeDelta(double longitudeDelta) {
        if (longitudeDelta > 180.0) {
            return longitudeDelta - 360.0;
        }
        if (longitudeDelta < -180.0) {
            return longitudeDelta + 360.0;
        }
        return longitudeDelta;
    }
}
