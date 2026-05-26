package core.scheduler;

import core.redis.RedisStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReminderDedupServiceTest {

    private RedisStore redisStore;
    private ReminderDedupService service;

    @BeforeEach
    void setUp() {
        redisStore = mock(RedisStore.class);
        service = new ReminderDedupService(redisStore);
    }

    @Test
    void alreadySentShouldReturnTrueWhenKeyExists() {
        when(redisStore.get("scheduler:", "key-1"))
                .thenReturn(Optional.of("1"));

        boolean result = service.alreadySent("key-1");

        assertTrue(result);
    }

    @Test
    void alreadySentShouldReturnFalseWhenKeyMissing() {
        when(redisStore.get("scheduler:", "key-1"))
                .thenReturn(Optional.empty());

        boolean result = service.alreadySent("key-1");

        assertFalse(result);
    }

    @Test
    void markSentShouldSaveForTwoDays() {
        service.markSent("key-1");

        verify(redisStore).put(
                eq("scheduler:"),
                eq("key-1"),
                eq("1"),
                eq(Duration.ofDays(2))
        );
    }

    @Test
    void markSentForDayShouldSaveForOneDay() {
        service.markSentForDay("key-1");

        verify(redisStore).put(
                eq("scheduler:"),
                eq("key-1"),
                eq("1"),
                eq(Duration.ofDays(1))
        );
    }
}