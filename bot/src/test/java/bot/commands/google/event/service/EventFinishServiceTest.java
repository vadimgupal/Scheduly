package bot.commands.google.event.service;

import bot.commands.google.event.state.EventFlowMode;
import bot.commands.google.event.state.EventStateStore;
import com.pengrad.telegrambot.TelegramBot;
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

class EventFinishServiceTest {

    private EventStateStore stateStore;
    private TelegramBot bot;
    private MockWebServer server;
    private EventFinishService service;

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

        service = new EventFinishService();

        ReflectionTestUtils.setField(service, "stateStore", stateStore);
        ReflectionTestUtils.setField(service, "webClient", webClient);
        ReflectionTestUtils.setField(service, "bot", bot);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void finishCreateShouldSendPostRequestAndClearState() throws Exception {
        String draft = String.join("\n----\n",
                "event",
                "description",
                "2026-05-25T14:00",
                "2026-05-25T15:00",
                "Europe/Moscow",
                "office",
                "60",
                "RRULE:FREQ=WEEKLY"
        );

        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of(draft));
        when(stateStore.getTargetCalendar(chatId))
                .thenReturn(Optional.of("calendar-1"));

        server.enqueue(new MockResponse().setResponseCode(201));

        service.finish(chatId, EventFlowMode.CREATE);

        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().startsWith("/event/create"));
        assertTrue(request.getPath().contains("chatId=123"));
        assertTrue(request.getPath().contains("calendarId=calendar-1"));

        String body = request.getBody().readUtf8();

        assertTrue(body.contains("event"));
        assertTrue(body.contains("description"));
        assertTrue(body.contains("[2026,5,25,14,0]"));
        assertTrue(body.contains("[2026,5,25,15,0]"));
        assertTrue(body.contains("Europe/Moscow"));
        assertTrue(body.contains("office"));
        assertTrue(body.contains("60"));
        assertTrue(body.contains("RRULE:FREQ=WEEKLY"));

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void finishCreateWithEmptyOptionalFieldsShouldSendNullsAndClearState() throws Exception {
        String draft = String.join("\n----\n",
                "event",
                "description",
                "2026-05-25T14:00",
                "2026-05-25T15:00",
                "",
                "",
                "",
                ""
        );

        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of(draft));
        when(stateStore.getTargetCalendar(chatId))
                .thenReturn(Optional.of("calendar-1"));

        server.enqueue(new MockResponse().setResponseCode(201));

        service.finish(chatId, EventFlowMode.CREATE);

        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().startsWith("/event/create"));

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void finishUpdateShouldSendPutRequestAndClearState() throws Exception {
        String draft = String.join("\n----\n",
                "updated event",
                "updated description",
                "2026-05-25T14:00",
                "2026-05-25T15:00",
                "Europe/Moscow",
                "office",
                "30",
                ""
        );

        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of(draft));
        when(stateStore.getTargetCalendar(chatId))
                .thenReturn(Optional.of("calendar-1"));
        when(stateStore.getTargetEvent(chatId))
                .thenReturn(Optional.of("event-1"));

        server.enqueue(new MockResponse().setResponseCode(204));

        service.finish(chatId, EventFlowMode.UPDATE);

        RecordedRequest request = server.takeRequest();

        assertEquals("PUT", request.getMethod());
        assertTrue(request.getPath().startsWith("/event/update"));
        assertTrue(request.getPath().contains("chatId=123"));
        assertTrue(request.getPath().contains("calendarId=calendar-1"));
        assertTrue(request.getPath().contains("eventId=event-1"));

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void finishWithWrongPartsCountShouldSendErrorAndClearState() {
        String draft = String.join("\n----\n",
                "event",
                "description",
                "2026-05-25T14:00"
        );

        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of(draft));

        service.finish(chatId, EventFlowMode.CREATE);

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void finishCreateWithoutCalendarShouldSendErrorAndClearState() {
        String draft = String.join("\n----\n",
                "event",
                "description",
                "2026-05-25T14:00",
                "2026-05-25T15:00",
                "Europe/Moscow",
                "office",
                "60",
                ""
        );

        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of(draft));
        when(stateStore.getTargetCalendar(chatId))
                .thenReturn(Optional.empty());

        service.finish(chatId, EventFlowMode.CREATE);

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void finishUpdateWithoutEventShouldSendErrorAndClearState() {
        String draft = String.join("\n----\n",
                "event",
                "description",
                "2026-05-25T14:00",
                "2026-05-25T15:00",
                "Europe/Moscow",
                "office",
                "60",
                ""
        );

        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of(draft));
        when(stateStore.getTargetCalendar(chatId))
                .thenReturn(Optional.of("calendar-1"));
        when(stateStore.getTargetEvent(chatId))
                .thenReturn(Optional.empty());

        service.finish(chatId, EventFlowMode.UPDATE);

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }

    @Test
    void finishWithInvalidReminderShouldSendErrorAndClearState() {
        String draft = String.join("\n----\n",
                "event",
                "description",
                "2026-05-25T14:00",
                "2026-05-25T15:00",
                "Europe/Moscow",
                "office",
                "abc",
                ""
        );

        when(stateStore.getDraft(chatId))
                .thenReturn(Optional.of(draft));

        service.finish(chatId, EventFlowMode.CREATE);

        verify(bot).execute(any(SendMessage.class));
        verify(stateStore).clear(chatId);
    }
}