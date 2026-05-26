package core.google;

import core.redis.RedisStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessTokenStoreTest {

    private RedisStore redis;
    private AccessTokenStore store;

    @BeforeEach
    void setUp() {
        redis = mock(RedisStore.class);
        store = new AccessTokenStore();
        ReflectionTestUtils.setField(store, "redis", redis);
    }

    @Test
    void putShouldSaveAccessToken() {
        Duration ttl = Duration.ofMinutes(10);

        store.put(1L, "access-token", ttl);

        verify(redis).put("token:", "1", "access-token", ttl);
    }

    @Test
    void getShouldReturnToken() {
        when(redis.get("token:", "1"))
                .thenReturn(Optional.of("access-token"));

        Optional<String> result = store.get("1");

        assertTrue(result.isPresent());
        assertEquals("access-token", result.get());
    }

    @Test
    void getShouldReturnEmptyWhenMissing() {
        when(redis.get("token:", "1"))
                .thenReturn(Optional.empty());

        Optional<String> result = store.get("1");

        assertTrue(result.isEmpty());
    }
}