import java.util.Objects;
import java.util.Optional;

public final class Bounds {
    private final Optional<Range> latitudeRange;
    private final Optional<Range> longitudeRange;

    public Bounds(double minLatitude, double maxLatitude,
                  double minLongitude, double maxLongitude) {
        this(Optional.of(Range.latitude(minLatitude, maxLatitude)),
                Optional.of(Range.longitude(minLongitude, maxLongitude)));
    }

    private Bounds(Optional<Range> latitudeRange, Optional<Range> longitudeRange) {
        this.latitudeRange = requireAxisRange(latitudeRange, true);
        this.longitudeRange = requireAxisRange(longitudeRange, false);
    }

    public static Bounds unbounded() {
        return new Bounds(Optional.empty(), Optional.empty());
    }

    public Optional<Range> getLatitudeRange() {
        return latitudeRange;
    }

    public Optional<Range> getLongitudeRange() {
        return longitudeRange;
    }

    public Bounds withLatitudeRange(Optional<Range> range) {
        return new Bounds(range, longitudeRange);
    }

    public Bounds withLongitudeRange(Optional<Range> range) {
        return new Bounds(latitudeRange, range);
    }

    public double getMinLatitude() {
        return latitudeRange.orElseThrow(() ->
                new IllegalStateException("Latitude range is not set")).getMinimum();
    }

    public double getMaxLatitude() {
        return latitudeRange.orElseThrow(() ->
                new IllegalStateException("Latitude range is not set")).getMaximum();
    }

    public double getMinLongitude() {
        return longitudeRange.orElseThrow(() ->
                new IllegalStateException("Longitude range is not set")).getMinimum();
    }

    public double getMaxLongitude() {
        return longitudeRange.orElseThrow(() ->
                new IllegalStateException("Longitude range is not set")).getMaximum();
    }

    public boolean contains(double latitude, double longitude) {
        return GpsValidation.isValidLatitude(latitude)
                && GpsValidation.isValidLongitude(longitude)
                && latitudeRange.map(range -> range.contains(latitude)).orElse(true)
                && longitudeRange.map(range -> range.contains(longitude)).orElse(true);
    }

    private static Optional<Range> requireAxisRange(Optional<Range> range, boolean latitude) {
        Objects.requireNonNull(range, "range");
        if (!range.isPresent()) {
            return Optional.empty();
        }
        if (range.get().isLatitude() != latitude) {
            throw new IllegalArgumentException("Range does not match coordinate axis");
        }
        return range;
    }

    public static final class Range {
        private final double minimum;
        private final double maximum;
        private final boolean latitude;

        private Range(double minimum, double maximum, boolean latitude) {
            boolean validMinimum = latitude
                    ? GpsValidation.isValidLatitude(minimum)
                    : GpsValidation.isValidLongitude(minimum);
            boolean validMaximum = latitude
                    ? GpsValidation.isValidLatitude(maximum)
                    : GpsValidation.isValidLongitude(maximum);
            if (!validMinimum || !validMaximum || minimum > maximum) {
                String axis = latitude ? "Latitude" : "Longitude";
                String limits = latitude ? "[-90, 90]" : "[-180, 180]";
                throw new IllegalArgumentException(axis + " bounds must be finite, in "
                        + limits + ", and min <= max");
            }
            this.minimum = minimum;
            this.maximum = maximum;
            this.latitude = latitude;
        }

        public static Range latitude(double minimum, double maximum) {
            return new Range(minimum, maximum, true);
        }

        public static Range longitude(double minimum, double maximum) {
            return new Range(minimum, maximum, false);
        }

        public double getMinimum() {
            return minimum;
        }

        public double getMaximum() {
            return maximum;
        }

        private boolean isLatitude() {
            return latitude;
        }

        private boolean contains(double value) {
            return value >= minimum && value <= maximum;
        }

        @Override
        public String toString() {
            return Double.toString(minimum) + " to " + Double.toString(maximum);
        }
    }
}
