package bot.commands.settings.state;

import bot.redis.RedisStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TimezoneStateStore {

    @Autowired
    private RedisStore redis;

    private static final Duration DURATION = Duration.ofMinutes(10);
    private static final String STATE_PREFIX = "timezone:state:";

    public void putState(long chatId, TimezoneState state) {
        redis.put(STATE_PREFIX, String.valueOf(chatId), state.name(), DURATION);
    }

    public Optional<TimezoneState> getState(long chatId) {
        return redis.get(STATE_PREFIX, String.valueOf(chatId))
                .map(TimezoneState::valueOf);
    }

    public void clear(long chatId) {
        redis.consume(STATE_PREFIX, String.valueOf(chatId));
    }
}