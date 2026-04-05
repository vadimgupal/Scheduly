package bot.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class RedisStore {
    @Autowired
    private StringRedisTemplate redis;

    public void put(String prefix, String key, String value, Duration ttl) {
        redis.opsForValue().set(prefix + key, value, ttl);
        log.info("[REDIS] PUT key='{}' ttl={}s value='{}'",
                prefix + key, ttl.toSeconds(), value);
    }

    public Optional<String> consume(String prefix, String key) {
        String res = redis.opsForValue().get(prefix + key);
        if(res == null) {
            log.info("[REDIS] CONSUME key='{}' -> MISS", prefix + key);
            return Optional.empty();
        }
        redis.delete(prefix + key);
        log.info("[REDIS] CONSUME key='{}' -> HIT value='{}' (deleted)",
                prefix + key, res);

        return Optional.of(res);
    }

    public Optional<String> get(String prefix, String key) {
        String res = redis.opsForValue().get(prefix + key);
        if(res == null) {
            log.info("[REDIS] GET key='{}' -> MISS", prefix + key);
            return Optional.empty();
        }
        log.info("[REDIS] GET key='{}' -> HIT value='{}'", prefix + key, res);
        return Optional.of(res);
    }
}
