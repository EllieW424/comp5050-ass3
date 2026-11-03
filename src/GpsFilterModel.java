import java.util.Objects;
import java.util.Optional;

import nz.sodium.Cell;
import nz.sodium.Stream;
import nz.sodium.Tuple2;

/** Maintains filter bounds and applies them to the unified GPS event stream. */
public final class GpsFilterModel {
    private static final FilterState INITIAL_STATE =
            new FilterState(Bounds.unbounded(), "", "");

    private final Cell<FilterState> state;
    private final Cell<Bounds> bounds;
    private final Stream<ReceivedGpsEvent> filteredEvents;

    public GpsFilterModel(Stream<ReceivedGpsEvent> events, Stream<Submission> submissions) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(submissions, "submissions");

        Stream<FilterState> stateUpdates = submissions.collect(INITIAL_STATE,
                (submission, previous) -> {
                    FilterState next = previous.apply(submission);
                    return new Tuple2<>(next, next);
                });
        this.state = stateUpdates.hold(INITIAL_STATE);
        this.bounds = state.map(FilterState::getBounds);
        Stream<Optional<ReceivedGpsEvent>> candidates = events.snapshot(bounds,
                (event, currentBounds) -> currentBounds.contains(
                        event.getLatitude(), event.getLongitude())
                                ? Optional.of(event) : Optional.empty());
        this.filteredEvents = Stream.filterOptional(candidates);
    }

    public Cell<Bounds> bounds() {
        return bounds;
    }

    public Cell<String> error(Axis axis) {
        Objects.requireNonNull(axis, "axis");
        return state.map(value -> value.getError(axis));
    }

    public Stream<ReceivedGpsEvent> filteredEvents() {
        return filteredEvents;
    }

    public enum Axis {
        LATITUDE,
        LONGITUDE
    }

    /** Raw field values are captured by snapshot when the corresponding button is clicked. */
    public static final class Submission {
        private final Axis axis;
        private final String minimum;
        private final String maximum;

        public Submission(Axis axis, String minimum, String maximum) {
            this.axis = Objects.requireNonNull(axis, "axis");
            this.minimum = Objects.requireNonNull(minimum, "minimum");
            this.maximum = Objects.requireNonNull(maximum, "maximum");
        }
    }

    private static final class FilterState {
        private final Bounds bounds;
        private final String latitudeError;
        private final String longitudeError;

        private FilterState(Bounds bounds, String latitudeError, String longitudeError) {
            this.bounds = bounds;
            this.latitudeError = latitudeError;
            this.longitudeError = longitudeError;
        }

        private FilterState apply(Submission submission) {
            String minimumText = submission.minimum.trim();
            String maximumText = submission.maximum.trim();
            String error = "";
            Optional<Bounds.Range> range = Optional.empty();

            if (minimumText.isEmpty() && maximumText.isEmpty()) {
                range = Optional.empty();
            } else if (minimumText.isEmpty() || maximumText.isEmpty()) {
                error = "Enter both limits, or leave both blank to clear this filter.";
            } else {
                try {
                    double minimum = Double.parseDouble(minimumText);
                    double maximum = Double.parseDouble(maximumText);
                    range = Optional.of(submission.axis == Axis.LATITUDE
                            ? Bounds.Range.latitude(minimum, maximum)
                            : Bounds.Range.longitude(minimum, maximum));
                } catch (NumberFormatException exception) {
                    error = "Enter valid numeric limits.";
                } catch (IllegalArgumentException exception) {
                    error = submission.axis == Axis.LATITUDE
                            ? "Latitude limits must be within -90 to 90, with Min no greater than Max."
                            : "Longitude limits must be within -180 to 180, with Min no greater than Max.";
                }
            }

            if (!error.isEmpty()) {
                return withError(submission.axis, error);
            }
            Bounds nextBounds = submission.axis == Axis.LATITUDE
                    ? bounds.withLatitudeRange(range)
                    : bounds.withLongitudeRange(range);
            return withBounds(submission.axis, nextBounds);
        }

        private FilterState withError(Axis axis, String error) {
            return axis == Axis.LATITUDE
                    ? new FilterState(bounds, error, longitudeError)
                    : new FilterState(bounds, latitudeError, error);
        }

        private FilterState withBounds(Axis axis, Bounds nextBounds) {
            return axis == Axis.LATITUDE
                    ? new FilterState(nextBounds, "", longitudeError)
                    : new FilterState(nextBounds, latitudeError, "");
        }

        private Bounds getBounds() {
            return bounds;
        }

        private String getError(Axis axis) {
            return axis == Axis.LATITUDE ? latitudeError : longitudeError;
        }
    }
}
