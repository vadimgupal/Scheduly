package bot.commands.google.event.state;

import bot.redis.RedisStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventStateStoreTest {

    private RedisStore redis;
    private EventStateStore store;

    @BeforeEach
    void setUp() {
        redis = mock(RedisStore.class);
        store = new EventStateStore();

        ReflectionTestUtils.setField(store, "redis", redis);
    }

    @Test
    void putStateShouldSaveStateToRedis() {
        long chatId = 123L;

        store.putState(chatId, EventState.EVENT_SUMMARY);

        verify(redis).put(
                eq("event:state:"),
                eq("123"),
                eq("EVENT_SUMMARY"),
                any(Duration.class)
        );
    }

    @Test
    void getStateShouldReturnStateFromRedis() {
        long chatId = 123L;

        when(redis.get("event:state:", "123"))
                .thenReturn(Optional.of("EVENT_SUMMARY"));

        Optional<EventState> result = store.getState(chatId);

        assertTrue(result.isPresent());
        assertEquals(EventState.EVENT_SUMMARY, result.get());
    }

    @Test
    void getStateShouldReturnEmptyWhenRedisEmpty() {
        long chatId = 123L;

        when(redis.get("event:state:", "123"))
                .thenReturn(Optional.empty());

        Optional<EventState> result = store.getState(chatId);

        assertTrue(result.isEmpty());
    }

    @Test
    void consumeStateShouldConsumeStateFromRedis() {
        long chatId = 123L;

        when(redis.consume("event:state:", "123"))
                .thenReturn(Optional.of("EVENT_DESCRIPTION"));

        Optional<EventState> result = store.consumeState(chatId);

        assertTrue(result.isPresent());
        assertEquals(EventState.EVENT_DESCRIPTION, result.get());
    }

    @Test
    void putModeShouldSaveModeToRedis() {
        long chatId = 123L;

        store.putMode(chatId, EventFlowMode.CREATE);

        verify(redis).put(
                eq("event:mode:"),
                eq("123"),
                eq("CREATE"),
                any(Duration.class)
        );
    }

    @Test
    void getModeShouldReturnModeFromRedis() {
        long chatId = 123L;

        when(redis.get("event:mode:", "123"))
                .thenReturn(Optional.of("UPDATE"));

        Optional<EventFlowMode> result = store.getMode(chatId);

        assertTrue(result.isPresent());
        assertEquals(EventFlowMode.UPDATE, result.get());
    }

    @Test
    void putDraftShouldSaveDraftToRedis() {
        long chatId = 123L;

        store.putDraft(chatId, "event\n----\ndescription");

        verify(redis).put(
                eq("event:draft:"),
                eq("123"),
                eq("event\n----\ndescription"),
                any(Duration.class)
        );
    }

    @Test
    void getDraftShouldReturnDraft() {
        long chatId = 123L;

        when(redis.get("event:draft:", "123"))
                .thenReturn(Optional.of("draft-value"));

        Optional<String> result = store.getDraft(chatId);

        assertTrue(result.isPresent());
        assertEquals("draft-value", result.get());
    }

    @Test
    void putTargetCalendarShouldSaveCalendarId() {
        long chatId = 123L;

        store.putTargetCalendar(chatId, "calendar-1");

        verify(redis).put(
                eq("event:calendar:"),
                eq("123"),
                eq("calendar-1"),
                any(Duration.class)
        );
    }

    @Test
    void getTargetCalendarShouldReturnCalendarId() {
        long chatId = 123L;

        when(redis.get("event:calendar:", "123"))
                .thenReturn(Optional.of("calendar-1"));

        Optional<String> result = store.getTargetCalendar(chatId);

        assertTrue(result.isPresent());
        assertEquals("calendar-1", result.get());
    }

    @Test
    void putOptionShouldSaveOptionByChatAndIndex() {
        long chatId = 123L;

        store.putOption(chatId, 2, "event-id");

        verify(redis).put(
                eq("event:options:123:"),
                eq("2"),
                eq("event-id"),
                any(Duration.class)
        );
    }

    @Test
    void getOptionShouldReturnOption() {
        long chatId = 123L;

        when(redis.get("event:options:123:", "2"))
                .thenReturn(Optional.of("event-id"));

        Optional<String> result = store.getOption(chatId, 2);

        assertTrue(result.isPresent());
        assertEquals("event-id", result.get());
    }

    @Test
    void putTargetEventShouldSaveEventId() {
        long chatId = 123L;

        store.putTargetEvent(chatId, "event-1");

        verify(redis).put(
                eq("event:event:"),
                eq("123"),
                eq("event-1"),
                any(Duration.class)
        );
    }

    @Test
    void getTargetEventShouldReturnEventId() {
        long chatId = 123L;

        when(redis.get("event:event:", "123"))
                .thenReturn(Optional.of("event-1"));

        Optional<String> result = store.getTargetEvent(chatId);

        assertTrue(result.isPresent());
        assertEquals("event-1", result.get());
    }

    @Test
    void clearShouldConsumeAllMainKeys() {
        long chatId = 123L;

        store.clear(chatId);

        verify(redis).consume("event:state:", "123");
        verify(redis).consume("event:mode:", "123");
        verify(redis).consume("event:draft:", "123");
        verify(redis).consume("event:calendar:", "123");
        verify(redis).consume("event:event:", "123");
    }
}