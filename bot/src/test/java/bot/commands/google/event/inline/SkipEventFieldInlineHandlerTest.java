package bot.commands.google.event.inline;

import bot.commands.google.event.service.EventFinishService;
import bot.commands.google.event.state.EventState;
import bot.commands.google.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SkipEventFieldInlineHandlerTest {

    private EventStateStore stateStore;
    private TelegramBot bot;
    private EventFinishService eventFinishService;
    private SkipEventFieldInlineHandler handler;

    private final long chatId = 123L;

    @BeforeEach
    void setUp() {
        stateStore = mock(EventStateStore.class);
        bot = mock(TelegramBot.class);
        eventFinishService = mock(EventFinishService.class);

        handler = new SkipEventFieldInlineHandler();

        ReflectionTestUtils.setField(handler, "stateStore", stateStore);
        ReflectionTestUtils.setField(handler, "bot", bot);
        ReflectionTestUtils.setField(handler, "eventFinishService", eventFinishService);
    }

    @Test
    void shouldHandleEventSkipCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        assertTrue(handler.shouldBeHandled(msg));
    }

    @Test
    void shouldNotHandleOtherCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:CANCEL", "callback-id");

        assertFalse(handler.shouldBeHandled(msg));
    }

    @Test
    void skipTimezoneShouldAppendEmptyAndMoveToLocation() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_TIMEZONE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("event\n----\ndescription\n----\n2026-05-25T14:00\n----\n2026-05-25T15:00"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId,
                "event\n----\ndescription\n----\n2026-05-25T14:00\n----\n2026-05-25T15:00\n----\n");
        verify(stateStore).putState(chatId, EventState.EVENT_LOCATION);
        verify(bot).execute(any(SendMessage.class));
        verify(bot).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void skipLocationShouldAppendEmptyAndMoveToReminder() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_LOCATION));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "draft\n----\n");
        verify(stateStore).putState(chatId, EventState.EVENT_REMINDER);
        verify(bot).execute(any(SendMessage.class));
        verify(bot).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void skipReminderShouldAppendEmptyAndMoveToRecurrence() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_REMINDER));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "draft\n----\n");
        verify(stateStore).putState(chatId, EventState.EVENT_RECURRENCE);
        verify(bot).execute(any(SendMessage.class));
        verify(bot).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void skipNotOptionalFieldShouldSendErrorAndNotChangeState() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_SUMMARY));

        handler.handle(msg);

        verify(stateStore, never()).putDraft(anyLong(), anyString());
        verify(stateStore, never()).putState(anyLong(), any(EventState.class));
        verify(bot).execute(any(SendMessage.class));
        verify(bot).execute(any(AnswerCallbackQuery.class));
    }
}