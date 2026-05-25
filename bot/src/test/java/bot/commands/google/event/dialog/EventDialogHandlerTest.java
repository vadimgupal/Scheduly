package bot.commands.google.event.dialog;

import bot.commands.google.event.state.EventFlowMode;
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

class EventDialogHandlerTest {

    private EventStateStore stateStore;
    private TelegramBot bot;
    private EventDialogHandler handler;

    private final long chatId = 123L;

    @BeforeEach
    void setUp() {
        stateStore = mock(EventStateStore.class);
        bot = mock(TelegramBot.class);

        handler = new EventDialogHandler();

        ReflectionTestUtils.setField(handler, "stateStore", stateStore);
        ReflectionTestUtils.setField(handler, "bot", bot);
    }

    @Test
    void shouldNotHandleCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        boolean result = handler.shouldBeHandled(msg);

        assertFalse(result);
    }

    @Test
    void shouldNotHandleCommandMessage() {
        UserMessage msg = UserMessage.text(chatId, "user", "/getEvents");

        boolean result = handler.shouldBeHandled(msg);

        assertFalse(result);
    }

    @Test
    void shouldHandleTextWhenStateIsEventSummary() {
        UserMessage msg = UserMessage.text(chatId, "user", "event");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_SUMMARY));

        boolean result = handler.shouldBeHandled(msg);

        assertTrue(result);
    }

    @Test
    void shouldNotHandleWhenStateIsSelectCalendar() {
        UserMessage msg = UserMessage.text(chatId, "user", "event");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.SELECT_CALENDAR));

        boolean result = handler.shouldBeHandled(msg);

        assertFalse(result);
    }

    @Test
    void handleSummaryShouldSaveDraftAndMoveToDescription() {
        UserMessage msg = UserMessage.text(chatId, "user", "event");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_SUMMARY));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "event");
        verify(stateStore).putState(chatId, EventState.EVENT_DESCRIPTION);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleBlankSummaryShouldNotChangeState() {
        UserMessage msg = UserMessage.text(chatId, "user", "   ");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_SUMMARY));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));

        handler.handle(msg);

        verify(stateStore, never()).putDraft(anyLong(), anyString());
        verify(stateStore, never()).putState(anyLong(), eq(EventState.EVENT_DESCRIPTION));
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleDescriptionShouldAppendDraftAndMoveToStartTime() {
        UserMessage msg = UserMessage.text(chatId, "user", "description");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_DESCRIPTION));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("event"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "event\n----\ndescription");
        verify(stateStore).putState(chatId, EventState.EVENT_START_TIME);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleStartTimeWithInvalidDateShouldNotMoveState() {
        UserMessage msg = UserMessage.text(chatId, "user", "bad-date");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_START_TIME));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));

        handler.handle(msg);

        verify(stateStore, never()).putState(chatId, EventState.EVENT_END_TIME);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleStartTimeWithValidDateShouldMoveToEndTime() {
        UserMessage msg = UserMessage.text(chatId, "user", "2026-05-25T14:00");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_START_TIME));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("event\n----\ndescription"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId,
                "event\n----\ndescription\n----\n2026-05-25T14:00");
        verify(stateStore).putState(chatId, EventState.EVENT_END_TIME);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleEndTimeBeforeStartShouldNotMoveState() {
        UserMessage msg = UserMessage.text(chatId, "user", "2026-05-25T13:00");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_END_TIME));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("event\n----\ndescription\n----\n2026-05-25T14:00"));

        handler.handle(msg);

        verify(stateStore, never()).putState(chatId, EventState.EVENT_TIMEZONE);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleEndTimeAfterStartShouldMoveToTimezone() {
        UserMessage msg = UserMessage.text(chatId, "user", "2026-05-25T15:00");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_END_TIME));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("event\n----\ndescription\n----\n2026-05-25T14:00"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId,
                "event\n----\ndescription\n----\n2026-05-25T14:00\n----\n2026-05-25T15:00");
        verify(stateStore).putState(chatId, EventState.EVENT_TIMEZONE);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleInvalidTimezoneShouldNotMoveState() {
        UserMessage msg = UserMessage.text(chatId, "user", "bad/timezone");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_TIMEZONE));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));

        handler.handle(msg);

        verify(stateStore, never()).putState(chatId, EventState.EVENT_LOCATION);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleValidTimezoneShouldMoveToLocation() {
        UserMessage msg = UserMessage.text(chatId, "user", "Europe/Moscow");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_TIMEZONE));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("event\n----\ndescription\n----\n2026-05-25T14:00\n----\n2026-05-25T15:00"));

        handler.handle(msg);

        verify(stateStore).putState(chatId, EventState.EVENT_LOCATION);
        verify(stateStore).putDraft(chatId,
                "event\n----\ndescription\n----\n2026-05-25T14:00\n----\n2026-05-25T15:00\n----\nEurope/Moscow");
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleLocationShouldMoveToReminder() {
        UserMessage msg = UserMessage.text(chatId, "user", "office");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_LOCATION));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "draft\n----\noffice");
        verify(stateStore).putState(chatId, EventState.EVENT_REMINDER);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleReminderWithNegativeNumberShouldNotMoveState() {
        UserMessage msg = UserMessage.text(chatId, "user", "-5");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_REMINDER));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));

        handler.handle(msg);

        verify(stateStore, never()).putState(chatId, EventState.EVENT_RECURRENCE);
        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void handleReminderWithValidNumberShouldMoveToRecurrence() {
        UserMessage msg = UserMessage.text(chatId, "user", "60");

        when(stateStore.getState(chatId))
                .thenReturn(Optional.of(EventState.EVENT_REMINDER));
        when(stateStore.getMode(chatId))
                .thenReturn(Optional.of(EventFlowMode.CREATE));
        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of("draft"));

        handler.handle(msg);

        verify(stateStore).putDraft(chatId, "draft\n----\n60");
        verify(stateStore).putState(chatId, EventState.EVENT_RECURRENCE);
        verify(bot).execute(any(SendMessage.class));
    }
}