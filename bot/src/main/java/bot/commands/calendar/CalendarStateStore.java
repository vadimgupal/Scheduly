package bot.commands.calendar;

import bot.redis.RedisStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CalendarStateStore {
    @Autowired
    private RedisStore redis;

    private static final Duration DURATION = Duration.ofMinutes(10);
    private static final String STATE_PREFIX = "dialog:state:";
    private static final String DRAFT_PREFIX = "dialog:draft:";
    private static final String TARGET_PREFIX = "dialog:target:";
    private static final String MODE_PREFIX = "dialog:mode:";
    private static final String OPTIONS_PREFIX = "dialog:options:";

    public void putState(long chatId, CalendarState state) {
        redis.put(STATE_PREFIX, String.valueOf(chatId), state.name(), DURATION);
    }

    public void putDraft(long chatId, String draft) {
        redis.put(DRAFT_PREFIX, String.valueOf(chatId), draft, DURATION);
    }

    public void putTarget(long chatId, String calendarId) {
        redis.put(TARGET_PREFIX, String.valueOf(chatId), calendarId, DURATION);
    }

    public void putMode(long chatId, CalendarFlowMode mode) {
        redis.put(MODE_PREFIX, String.valueOf(chatId), mode.name(), DURATION);
    }

    public void putOption(long chatId, int index, String calendarId) {
        redis.put(OPTIONS_PREFIX, chatId + ":" + index, calendarId, DURATION);
    }

    public Optional<String> getOption(long chatId, int index) {
        return redis.get(OPTIONS_PREFIX, chatId + ":" + index);
    }

    public Optional<CalendarFlowMode> getMode(long chatId) {
        return redis.get(MODE_PREFIX, String.valueOf(chatId)).map(CalendarFlowMode::valueOf);
    }

    public Optional<String> getTarget(long chatId) {
        return redis.get(TARGET_PREFIX, String.valueOf(chatId));
    }

    public Optional<CalendarState> getState(long chatId) {
        return redis.get(STATE_PREFIX, String.valueOf(chatId)).map(CalendarState::valueOf);
    }

    public Optional<String> getDraft(long chatId) {
        return redis.get(DRAFT_PREFIX, String.valueOf(chatId));
    }

    public Optional<CalendarState> consumeState(long chatId) {
        return redis.consume(STATE_PREFIX, String.valueOf(chatId)).map(CalendarState::valueOf);
    }

    public void clear(long chatId) {
        redis.consume(STATE_PREFIX, String.valueOf(chatId));
        redis.consume(DRAFT_PREFIX, String.valueOf(chatId));
        redis.consume(TARGET_PREFIX, String.valueOf(chatId));
        redis.consume(MODE_PREFIX, String.valueOf(chatId));
    }
}
