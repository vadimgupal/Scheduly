package core.google;

import core.redis.RedisStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OAuthStateStoreTest {

    private RedisStore redis;
    private OAuthStateStore store;

    @BeforeEach
    void setUp() {
        redis = mock(RedisStore.class);
        store = new OAuthStateStore();
        ReflectionTestUtils.setField(store, "redis", redis);
    }

    @Test
    void putShouldSaveStateWithTtl() {
        store.put("state-1", 123L);

        verify(redis).put(
                eq("oauth:state:"),
                eq("state-1"),
                eq("123"),
                any(Duration.class)
        );
    }

    @Test
    void consumeShouldReturnChatIdWhenValueExists() {
        when(redis.consume("oauth:state:", "state-1"))
                .thenReturn(Optional.of("123"));

        Optional<Long> result = store.consume("state-1");

        assertTrue(result.isPresent());
        assertEquals(123L, result.get());
    }

    @Test
    void consumeShouldReturnEmptyWhenMissing() {
        when(redis.consume("oauth:state:", "state-1"))
                .thenReturn(Optional.empty());

        Optional<Long> result = store.consume("state-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void consumeShouldReturnEmptyWhenValueIsNotNumber() {
        when(redis.consume("oauth:state:", "state-1"))
                .thenReturn(Optional.of("abc"));

        Optional<Long> result = store.consume("state-1");

        assertTrue(result.isEmpty());
    }
}