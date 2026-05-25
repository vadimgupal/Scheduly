package bot.commands.google.schedule.state;

import bot.redis.RedisStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class FreeSlotsStateStore {

    private final RedisStore redis;

    private static final String PREFIX = "free_slots:state:";
    private static final Duration TTL = Duration.ofMinutes(10);

    public FreeSlotsStateStore(RedisStore redis) {
        this.redis = redis;
    }

    public void putState(long chatId, FreeSlotsState state) {
        redis.put(PREFIX, String.valueOf(chatId), state.name(), TTL);
    }

    public Optional<FreeSlotsState> getState(long chatId) {
        return redis.get(PREFIX, String.valueOf(chatId))
                .map(FreeSlotsState::valueOf);
    }

    public void clear(long chatId) {
        redis.consume(PREFIX, String.valueOf(chatId));
    }
}