package bot.commands.event.state;

import bot.redis.RedisStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class EventStateStore {

    @Autowired
    private RedisStore redis;

    private static final Duration DURATION = Duration.ofMinutes(10);

    private static final String STATE_PREFIX = "event:state:";
    private static final String MODE_PREFIX = "event:mode:";
    private static final String DRAFT_PREFIX = "event:draft:";
    private static final String TARGET_PREFIX = "event:calendar:";
    private static final String OPTIONS_PREFIX = "event:options:";
    private static final String EVENT_PREFIX = "event:event:";

    public void putState(long chatId, EventState state) {
        redis.put(STATE_PREFIX, String.valueOf(chatId), state.name(), DURATION);
    }

    public Optional<EventState> getState(long chatId) {
        return redis.get(STATE_PREFIX, String.valueOf(chatId))
                .map(EventState::valueOf);
    }

    public Optional<EventState> consumeState(long chatId) {
        return redis.consume(STATE_PREFIX, String.valueOf(chatId))
                .map(EventState::valueOf);
    }

    public void putMode(long chatId, EventFlowMode mode) {
        redis.put(MODE_PREFIX, String.valueOf(chatId), mode.name(), DURATION);
    }

    public Optional<EventFlowMode> getMode(long chatId) {
        return redis.get(MODE_PREFIX, String.valueOf(chatId))
                .map(EventFlowMode::valueOf);
    }

    public void putDraft(long chatId, String draft) {
        redis.put(DRAFT_PREFIX, String.valueOf(chatId), draft, DURATION);
    }

    public void putOption(long chatId, int index, String value) {
        redis.put(OPTIONS_PREFIX + chatId + ":", String.valueOf(index), value, DURATION);
    }

    public Optional<String> getDraft(long chatId) {
        return redis.get(DRAFT_PREFIX, String.valueOf(chatId));
    }

    public void putTargetCalendar(long chatId, String calendarId) {
        redis.put(TARGET_PREFIX, String.valueOf(chatId), calendarId, DURATION);
    }

    public Optional<String> getTargetCalendar(long chatId) {
        return redis.get(TARGET_PREFIX, String.valueOf(chatId));
    }

    public Optional<String> getOption(long chatId, int index) {
        return redis.get(OPTIONS_PREFIX + chatId + ":", String.valueOf(index));
    }

    public void putTargetEvent(long chatId, String eventId) {
        redis.put(EVENT_PREFIX, String.valueOf(chatId), eventId, DURATION);
    }

    public Optional<String> getTargetEvent(long chatId) {
        return redis.get(EVENT_PREFIX, String.valueOf(chatId));
    }

    public void clear(long chatId) {
        redis.consume(STATE_PREFIX, String.valueOf(chatId));
        redis.consume(MODE_PREFIX, String.valueOf(chatId));
        redis.consume(DRAFT_PREFIX, String.valueOf(chatId));
        redis.consume(TARGET_PREFIX, String.valueOf(chatId));
        redis.consume(EVENT_PREFIX, String.valueOf(chatId));
    }
}