package core.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisStore {
    @Autowired
    private StringRedisTemplate redis;

    public void put(String prefix, String key, String value, Duration ttl) {
        redis.opsForValue().set(prefix + key, value, ttl);
    }

    public Optional<String> consume(String prefix, String key) {
        String res = redis.opsForValue().get(prefix + key);
        if(res == null) {
            return Optional.empty();
        }
        redis.delete(prefix + key);

        return Optional.of(res);
    }

    public Optional<String> get(String prefix, String key) {
        String res = redis.opsForValue().get(prefix + key);
        if(res == null) {
            return Optional.empty();
        }
        return Optional.of(res);
    }
}
