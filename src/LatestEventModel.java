import java.util.Objects;
import java.util.Optional;

import nz.sodium.Cell;
import nz.sodium.Stream;
import nz.sodium.Tuple2;

/** Tracks the most recent event until its matching timeout token arrives. */
public final class LatestEventModel {
    public static final int DISPLAY_TIMEOUT_MILLIS = 3_000;

    private final Cell<Optional<String>> latestEvent;
    private final Cell<Optional<TimeoutRequest>> currentTimeout;
    private final Stream<TimeoutRequest> timeoutRequests;

    public LatestEventModel(Stream<ReceivedGpsEvent> events, Stream<Long> timeoutTicks) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(timeoutTicks, "timeoutTicks");

        this.timeoutRequests = events.collect(0L,
                (event, previousGeneration) -> {
                    long generation = previousGeneration + 1L;
                    String text = FormattedEvent.from(event).format();
                    return new Tuple2<>(new TimeoutRequest(generation, text), generation);
                });
        this.currentTimeout = timeoutRequests.map(Optional::of).hold(Optional.empty());

        Cell<Long> currentGeneration = timeoutRequests
                .map(TimeoutRequest::getGeneration).hold(0L);

        Stream<Boolean> matchingTicks = timeoutTicks.snapshot(currentGeneration,
                (token, generation) -> token.longValue() == generation.longValue());
        Stream<Optional<String>> clearEvents = matchingTicks
                .filter(matches -> matches)
                .mapTo(Optional.<String>empty());
        Stream<Optional<String>> replaceEvents = timeoutRequests
                .map(request -> Optional.of(request.getText()));
        this.latestEvent = replaceEvents.orElse(clearEvents).hold(Optional.empty());
    }

    public Cell<Optional<String>> latestEvent() {
        return latestEvent;
    }

    public Stream<TimeoutRequest> timeoutRequests() {
        return timeoutRequests;
    }

    public Cell<Optional<TimeoutRequest>> currentTimeout() {
        return currentTimeout;
    }

    /** A timeout may clear the display only while its event is still current. */
    public static final class TimeoutRequest {
        private final long generation;
        private final String text;

        private TimeoutRequest(long generation, String text) {
            this.generation = generation;
            this.text = text;
        }

        public long getGeneration() {
            return generation;
        }

        public String getText() {
            return text;
        }
    }
}
