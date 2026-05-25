package bot.commands.google.event.inline;

import bot.commands.google.event.service.EventFinishService;
import bot.commands.google.event.state.EventFlowMode;
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

class SelectRecurrenceInlineHandlerTest {

    private EventStateStore stateStore;
    private TelegramBot bot;
    private EventFinishService eventFinishService;
    private SelectRecurrenceInlineHandler handler;

    private final long chatId = 123L;

    @BeforeEach
    void setUp() {
        stateStore = mock(EventStateStore.class);
        bot = mock(TelegramBot.class);
        eventFinishService = mock(EventFinishService.class);

        handler = new SelectRecurrenceInlineHandler();

        ReflectionTestUtils.setField(handler, "stateStore", stateStore);
        ReflectionTestUtils.setField(handler, "bot", bot);
        ReflectionTestUtils.setField(handler, "eventFinishService", eventFinishService);
    }

    @Test
    void shouldHandleRecurrenceCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:RECURRENCE:WEEKLY", "callback-id");

        assertTrue(handler.shouldBeHandled(msg));
    }

    @Test
    void shouldNotHandleOtherCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        assertFalse(handler.shouldBeHandled(msg));
    }

    @Test
    void shouldRejectWhenStateIsNotRecurrence() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:RECURRENCE:WEEKLY", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_REMINDER));

        handler.handle(msg);

        verify(bot).execute(any(SendMessage.class));
        verify(eventFinishService, never()).finish(anyLong(), any());
    }

    @Test
    void weeklyShouldAppendRruleAndFinish() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:RECURRENCE:WEEKLY", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_RECURRENCE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "draft\n----\nRRULE:FREQ=WEEKLY");
        verify(bot).execute(any(AnswerCallbackQuery.class));
        verify(eventFinishService).finish(chatId, EventFlowMode.CREATE);
    }

    @Test
    void noneShouldAppendEmptyStringAndFinish() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:RECURRENCE:NONE", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_RECURRENCE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "draft\n----\n");
        verify(eventFinishService).finish(chatId, EventFlowMode.CREATE);
    }

    @Test
    void dailyShouldAppendDailyRrule() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:RECURRENCE:DAILY", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_RECURRENCE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.UPDATE));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "draft\n----\nRRULE:FREQ=DAILY");
        verify(eventFinishService).finish(chatId, EventFlowMode.UPDATE);
    }

    @Test
    void unknownRecurrenceShouldThrowException() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:RECURRENCE:SOMETHING", "callback-id");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_RECURRENCE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));

        assertThrows(IllegalArgumentException.class, () -> handler.handle(msg));

        verify(eventFinishService, never()).finish(anyLong(), any());
    }
}