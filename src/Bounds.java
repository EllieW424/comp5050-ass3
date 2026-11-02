public final class Bounds {
    private final double minLatitude;
    private final double maxLatitude;
    private final double minLongitude;
    private final double maxLongitude;

    public Bounds(double minLatitude, double maxLatitude,
                  double minLongitude, double maxLongitude) {
        if (!GpsValidation.isValidLatitude(minLatitude)
                || !GpsValidation.isValidLatitude(maxLatitude)
                || minLatitude > maxLatitude) {
            throw new IllegalArgumentException("Latitude bounds must be finite, in [-90, 90], and min <= max");
        }
        if (!GpsValidation.isValidLongitude(minLongitude)
                || !GpsValidation.isValidLongitude(maxLongitude)
                || minLongitude > maxLongitude) {
            throw new IllegalArgumentException("Longitude bounds must be finite, in [-180, 180], and min <= max");
        }
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

    public boolean contains(double latitude, double longitude) {
        return GpsValidation.isValidLatitude(latitude)
                && GpsValidation.isValidLongitude(longitude)
                && latitude >= minLatitude && latitude <= maxLatitude
                && longitude >= minLongitude && longitude <= maxLongitude;
    }
}
