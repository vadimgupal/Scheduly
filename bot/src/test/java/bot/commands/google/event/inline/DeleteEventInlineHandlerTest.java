package bot.commands.google.event.inline;

import bot.commands.google.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteEventInlineHandlerTest {

    private EventStateStore stateStore;
    private TelegramBot bot;
    private MockWebServer server;
    private DeleteEventInlineHandler handler;

    private final long chatId = 123L;

    @BeforeEach
    void setUp() throws Exception {
        stateStore = mock(EventStateStore.class);
        bot = mock(TelegramBot.class);

        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();

        handler = new DeleteEventInlineHandler();

        ReflectionTestUtils.setField(handler, "stateStore", stateStore);
        ReflectionTestUtils.setField(handler, "bot", bot);
        ReflectionTestUtils.setField(handler, "webClient", webClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void shouldHandleDeleteEventCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:DELETE_EVENT:0", "callback-id");

        assertTrue(handler.shouldBeHandled(msg));
    }

    @Test
    void shouldNotHandleOtherCallback() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:SELECT_EVENT:0", "callback-id");

        assertFalse(handler.shouldBeHandled(msg));
    }

    @Test
    void handleShouldDeleteEventAndClearState() throws Exception {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:DELETE_EVENT:0", "callback-id");

        when(stateStore.getOption(chatId, 0))
                .thenReturn(Optional.of("event-1"));
        when(stateStore.getTargetCalendar(chatId))
                .thenReturn(Optional.of("calendar-1"));

        server.enqueue(new MockResponse().setResponseCode(200));

        handler.handle(msg);

        RecordedRequest request = server.takeRequest();

        assertEquals("DELETE", request.getMethod());
        assertTrue(request.getPath().startsWith("/event/delete"));
        assertTrue(request.getPath().contains("chatId=123"));
        assertTrue(request.getPath().contains("calendarId=calendar-1"));
        assertTrue(request.getPath().contains("eventId=event-1"));

        verify(bot).execute(any(AnswerCallbackQuery.class));
        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void handleWhenOptionMissingShouldSendErrorAndClearState() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:DELETE_EVENT:0", "callback-id");

        when(stateStore.getOption(chatId, 0))
                .thenReturn(Optional.empty());

        handler.handle(msg);

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void handleWhenCalendarMissingShouldSendErrorAndClearState() {
        UserMessage msg = UserMessage.callback(chatId, "user", "EVENT:DELETE_EVENT:0", "callback-id");

        when(stateStore.getOption(chatId, 0))
                .thenReturn(Optional.of("event-1"));
        when(stateStore.getTargetCalendar(chatId))
                .thenReturn(Optional.empty());

        handler.handle(msg);

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }
}