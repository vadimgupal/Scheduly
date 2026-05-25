package core.scheduler;

import core.redis.RedisStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class ReminderDedupService {

    private final RedisStore redisStore;

    public ReminderDedupService(RedisStore redisStore) {
        this.redisStore = redisStore;
    }

    public boolean alreadySent(String key) {
        boolean result = redisStore.get("scheduler:", key).isPresent();
        log.info("[REMINDER_REDIS] alreadySent key={} result={}", key, result);
        return result;
    }

    public void markSent(String key) {
        redisStore.put("scheduler:", key, "1", Duration.ofDays(2));
    }

    public void markSentForDay(String key) {
        redisStore.put("scheduler:", key, "1", Duration.ofDays(1));
    }
}