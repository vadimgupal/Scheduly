package core.google;

import core.dto.GoogleEventDateTime;
import core.dto.GoogleEventItem;
import dto.Event;
import dto.EventListItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GoogleEventMapperTest {

    private GoogleEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GoogleEventMapper(WebClient.builder().build());
    }

    @Test
    void toGoogleEventBodyShouldMapEventWithTimezone() {
        Event event = new Event(
                "summary",
                "description",
                LocalDateTime.of(2026, 5, 25, 14, 0),
                LocalDateTime.of(2026, 5, 25, 15, 0),
                "Europe/Moscow",
                "office",
                60,
                "RRULE:FREQ=WEEKLY"
        );

        Map<String, Object> body = mapper.toGoogleEventBody(event, "token", "calendar-1");

        assertEquals("summary", body.get("summary"));
        assertEquals("description", body.get("description"));
        assertEquals("office", body.get("location"));

        Map<?, ?> start = (Map<?, ?>) body.get("start");
        Map<?, ?> end = (Map<?, ?>) body.get("end");

        assertEquals("2026-05-25T14:00:00", start.get("dateTime"));
        assertEquals("Europe/Moscow", start.get("timeZone"));

        assertEquals("2026-05-25T15:00:00", end.get("dateTime"));
        assertEquals("Europe/Moscow", end.get("timeZone"));

        Map<?, ?> reminders = (Map<?, ?>) body.get("reminders");
        assertEquals(false, reminders.get("useDefault"));

        List<?> recurrence = (List<?>) body.get("recurrence");
        assertEquals("RRULE:FREQ=WEEKLY", recurrence.getFirst());
    }

    @Test
    void toGoogleEventBodyShouldSkipOptionalLocationReminderAndRecurrence() {
        Event event = new Event(
                "summary",
                "description",
                LocalDateTime.of(2026, 5, 25, 14, 0),
                LocalDateTime.of(2026, 5, 25, 15, 0),
                "Europe/Moscow",
                null,
                null,
                null
        );

        Map<String, Object> body = mapper.toGoogleEventBody(event, "token", "calendar-1");

        assertFalse(body.containsKey("location"));
        assertFalse(body.containsKey("reminders"));
        assertFalse(body.containsKey("recurrence"));
    }

    @Test
    void toEventListItemDtoShouldMapGoogleEvent() {
        GoogleEventItem item = new GoogleEventItem(
                "event-1",
                "confirmed",
                "summary",
                "description",
                new GoogleEventDateTime("2026-05-25T14:00:00+03:00", null, "Europe/Moscow"),
                new GoogleEventDateTime("2026-05-25T15:00:00+03:00", null, "Europe/Moscow"),
                "office",
                Map.of(
                        "overrides",
                        List.of(Map.of("method", "popup", "minutes", 60))
                ),
                List.of("RRULE:FREQ=WEEKLY")
        );

        EventListItemDto dto = mapper.toEventListItemDto(item);

        assertEquals("event-1", dto.id());
        assertEquals("summary", dto.name());
        assertEquals("description", dto.description());
        assertEquals(LocalDateTime.of(2026, 5, 25, 14, 0), dto.start());
        assertEquals(LocalDateTime.of(2026, 5, 25, 15, 0), dto.end());
        assertEquals("office", dto.location());
        assertEquals(60, dto.reminderMinutesBefore());
        assertEquals("RRULE:FREQ=WEEKLY", dto.recurrenceRule());
    }

    @Test
    void toEventListItemDtoShouldMapAllDayDate() {
        GoogleEventItem item = new GoogleEventItem(
                "event-1",
                "confirmed",
                "summary",
                null,
                new GoogleEventDateTime(null, "2026-05-25", null),
                new GoogleEventDateTime(null, "2026-05-26", null),
                null,
                null,
                null
        );

        EventListItemDto dto = mapper.toEventListItemDto(item);

        assertEquals(LocalDateTime.of(2026, 5, 25, 0, 0), dto.start());
        assertEquals(LocalDateTime.of(2026, 5, 26, 0, 0), dto.end());
        assertNull(dto.reminderMinutesBefore());
        assertNull(dto.recurrenceRule());
    }
}