import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

import nz.sodium.Stream;

/** Connects the course service streams to one timestamped event stream. */
public final class GpsServiceAdapter {
    private final GpsService service;
    private final Stream<ReceivedGpsEvent> events;

    /** Creates exactly one course service and subscribes to every stream it exposes. */
    public GpsServiceAdapter() {
        this(new GpsService(), System::nanoTime);
    }

    public GpsServiceAdapter(GpsService service) {
        this(service, System::nanoTime);
    }

    private GpsServiceAdapter(GpsService service, LongSupplier clock) {
        this.service = Objects.requireNonNull(service, "service");
        this.events = merge(service.getEventStreams(), clock);
    }

    /** Test seam that avoids constructing the timer-backed course service. */
    public GpsServiceAdapter(Stream<GpsEvent>[] streams, LongSupplier clock) {
        this.service = null;
        this.events = merge(streams, clock);
    }

    public Stream<ReceivedGpsEvent> events() {
        return events;
    }

    public Stream<ReceivedGpsEvent> getEvents() {
        return events();
    }

    public GpsService getService() {
        return service;
    }

    private static Stream<ReceivedGpsEvent> merge(Stream<GpsEvent>[] streams,
                                                   LongSupplier clock) {
        Objects.requireNonNull(streams, "streams");
        Objects.requireNonNull(clock, "clock");

        List<Stream<ReceivedGpsEvent>> mapped = new ArrayList<>();
        for (Stream<GpsEvent> stream : Arrays.asList(streams)) {
            if (stream == null) {
                throw new IllegalArgumentException("GpsService returned a null stream");
            }
            mapped.add(stream.map(event -> ReceivedGpsEvent.from(event, clock.getAsLong())));
        }
        if (mapped.isEmpty()) {
            return new Stream<>();
        }
        if (mapped.size() == 1) {
            return mapped.get(0);
        }
        return Stream.merge(mapped, (first, second) -> second);
    }
}
