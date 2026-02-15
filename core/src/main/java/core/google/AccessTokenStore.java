package core.google;

import core.redis.RedisStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class AccessTokenStore {

    @Autowired
    private RedisStore redis;

    private static final String PREFIX = "token:";

    public void put(long key, String value, Duration ttl) {
        redis.put(PREFIX, Long.toString(key), value, ttl);
    }

    public Optional<String> get(String key) {
        return redis.get(PREFIX, key);
    }
}
