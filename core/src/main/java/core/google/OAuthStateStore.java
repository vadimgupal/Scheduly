package core.google;

import core.redis.RedisStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class OAuthStateStore {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String PREFIX = "oauth:state:";

    @Autowired
    private RedisStore redis;

    public void put(String state, long chatId){
        redis.put(PREFIX, state, Long.toString(chatId), TTL);
    }

    public Optional<Long> consume(String state) {
        Optional<String> res = redis.consume(PREFIX, state);
        if (res.isEmpty()) {
            return Optional.empty();
        }

        try{
            return Optional.of(Long.parseLong(res.get()));
        } catch (NumberFormatException e){
            return Optional.empty();
        }
    }
}
