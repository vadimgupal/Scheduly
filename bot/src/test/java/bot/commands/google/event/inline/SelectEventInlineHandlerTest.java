package bot.commands.google.event.inline;

import bot.commands.google.event.state.EventState;
import bot.commands.google.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SelectEventInlineHandlerTest {

    private EventStateStore stateStore;
    private TelegramBot bot;
    private SelectEventInlineHandler handler;

    private final long chatId = 123L;

    @BeforeEach
    void setUp() {
        stateStore = mock(EventStateStore.class);
        bot = mock(TelegramBot.class);

        handler = new SelectEventInlineHandler();

        ReflectionTestUtils.setField(handler, "stateStore", stateStore);
        ReflectionTestUtils.setField(handler, "bot", bot);
    }

    @Test
    void shouldHandleSelectEventCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SELECT_EVENT:0", "callback-id");

        assertTrue(handler.shouldBeHandled(msg));
    }

    @Test
    void shouldNotHandleOtherCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:DELETE_EVENT:0", "callback-id");

        assertFalse(handler.shouldBeHandled(msg));
    }

    @Test
    void handleShouldIgnoreWhenStateIsNotSelectEvent() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SELECT_EVENT:0", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_SUMMARY));

        handler.handle(msg);

        verify(stateStore, never()).putTargetEvent(anyLong(), anyString());
        verify(stateStore, never()).putState(anyLong(), any(EventState.class));
        verify(bot, never()).execute(any(SendMessage.class));
    }

    @Test
    void handleShouldSaveTargetEventAndMoveToSummary() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SELECT_EVENT:0", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.SELECT_EVENT));
        when(stateStore.getOption(chatId, 0))
                .thenReturn(Optional.of("event-1"));

        handler.handle(msg);

        verify(stateStore).putTargetEvent(chatId, "event-1");
        verify(stateStore).putState(chatId, EventState.EVENT_SUMMARY);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleShouldThrowWhenOptionMissing() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SELECT_EVENT:0", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.SELECT_EVENT));
        when(stateStore.getOption(chatId, 0))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> handler.handle(msg));
    }
}