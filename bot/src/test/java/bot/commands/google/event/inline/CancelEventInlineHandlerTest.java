package bot.commands.google.event.inline;

import bot.commands.google.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CancelEventInlineHandlerTest {

    private EventStateStore stateStore;
    private TelegramBot bot;
    private CancelEventInlineHandler handler;

    private final long chatId = 123L;

    @BeforeEach
    void setUp() {
        stateStore = mock(EventStateStore.class);
        bot = mock(TelegramBot.class);

        handler = new CancelEventInlineHandler();

        ReflectionTestUtils.setField(handler, "stateStore", stateStore);
        ReflectionTestUtils.setField(handler, "bot", bot);
    }

    @Test
    void shouldHandleCancelCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:CANCEL", "callback-id");

        assertTrue(handler.shouldBeHandled(msg));
    }

    @Test
    void shouldNotHandleOtherCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SKIP", "callback-id");

        assertFalse(handler.shouldBeHandled(msg));
    }

    @Test
    void handleShouldClearStateAndSendMessages() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:CANCEL", "callback-id");

        handler.handle(msg);

        verify(stateStore).clear(chatId);
        verify(bot).execute(any(AnswerCallbackQuery.class));
        verify(bot).execute(any(SendMessage.class));
    }
}