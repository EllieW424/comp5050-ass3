import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import nz.sodium.Listener;
import nz.sodium.Stream;
import nz.sodium.StreamSink;

public class LatestEvent_Test {
    @Test
    public void displaysAnEventImmediatelyAndReplacesItWithTheNextEvent() {
        Fixture fixture = new Fixture();

        fixture.sendEvent("Tracker1", 1.25, -2.5, 30.0);
        assertEvent(fixture, "Tracker1,1.25,-2.5,30.0");

        fixture.sendEvent("Tracker2", -4.0, 5.75, 60.0);
        assertEvent(fixture, "Tracker2,-4.0,5.75,60.0");
        assertEquals(2, fixture.requests.size());
    }

    @Test
    public void secondEventRemainsVisibleUntilItsOwnThreeSecondDeadline() {
        Fixture fixture = new Fixture();

        fixture.sendEvent("Tracker1", 1.0, 2.0, 3.0);
        fixture.timer.advanceBy(2_000L);
        fixture.sendEvent("Tracker2", 4.0, 5.0, 6.0);
        assertEvent(fixture, "Tracker2,4.0,5.0,6.0");

        fixture.timer.advanceBy(2_900L);
        assertEvent(fixture, "Tracker2,4.0,5.0,6.0");

        fixture.timer.advanceBy(100L);
        assertFalse(fixture.model.latestEvent().sample().isPresent());
    }

    @Test
    public void delayedTimeoutFromOlderEventCannotClearTheNewerEvent() {
        Fixture fixture = new Fixture();

        fixture.sendEvent("Tracker1", 1.0, 2.0, 3.0);
        long oldGeneration = fixture.requests.get(0).getGeneration();
        fixture.sendEvent("Tracker2", 4.0, 5.0, 6.0);
        long currentGeneration = fixture.requests.get(1).getGeneration();

        fixture.timer.fire(oldGeneration);
        assertEvent(fixture, "Tracker2,4.0,5.0,6.0");

        fixture.timer.fire(currentGeneration);
        assertFalse(fixture.model.latestEvent().sample().isPresent());
    }

    private static void assertEvent(Fixture fixture, String expected) {
        assertEquals(Optional.of(expected), fixture.model.latestEvent().sample());
    }

    private static final class Fixture {
        private final StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        private final StreamSink<Long> ticks = new StreamSink<>();
        private final LatestEventModel model = new LatestEventModel(events, ticks);
        private final List<LatestEventModel.TimeoutRequest> requests = new ArrayList<>();
        private final VirtualTimer timer;

        private Fixture() {
            model.timeoutRequests().listen(requests::add);
            timer = new VirtualTimer(model.timeoutRequests(), ticks);
        }

        private void sendEvent(String tracker, double latitude, double longitude,
                               double altitudeFeet) {
            events.send(ReceivedGpsEvent.from(
                    new GpsEvent(tracker, latitude, longitude, altitudeFeet),
                    timer.nowMillis() * 1_000_000L));
        }
    }

    private static final class VirtualTimer {
        private final StreamSink<Long> ticks;
        private final List<ScheduledTick> scheduledTicks = new ArrayList<>();
        private final Listener listener;
        private long nowMillis;

        private VirtualTimer(Stream<LatestEventModel.TimeoutRequest> requests,
                             StreamSink<Long> ticks) {
            this.ticks = ticks;
            this.listener = requests.listen(request -> scheduledTicks.add(
                    new ScheduledTick(nowMillis + LatestEventModel.DISPLAY_TIMEOUT_MILLIS,
                            request.getGeneration())));
        }

        private long nowMillis() {
            return nowMillis;
        }

        private void advanceBy(long milliseconds) {
            long targetMillis = nowMillis + milliseconds;
            while (true) {
                int nextTick = nextDueTick(targetMillis);
                if (nextTick < 0) {
                    break;
                }
                ScheduledTick scheduled = scheduledTicks.remove(nextTick);
                nowMillis = scheduled.deadlineMillis;
                ticks.send(scheduled.generation);
            }
            nowMillis = targetMillis;
        }

        private void fire(long generation) {
            ticks.send(generation);
        }

        private int nextDueTick(long targetMillis) {
            int next = -1;
            for (int index = 0; index < scheduledTicks.size(); index++) {
                if (scheduledTicks.get(index).deadlineMillis <= targetMillis
                        && (next < 0 || scheduledTicks.get(index).deadlineMillis
                                < scheduledTicks.get(next).deadlineMillis)) {
                    next = index;
                }
            }
            return next;
        }
    }

    private static final class ScheduledTick {
        private final long deadlineMillis;
        private final long generation;

        private ScheduledTick(long deadlineMillis, long generation) {
            this.deadlineMillis = deadlineMillis;
            this.generation = generation;
        }
    }
}
