package core.google;

import core.DTO.GoogleEventDateTime;
import core.DTO.GoogleEventItem;
import dto.Event;
import dto.EventListItemDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GoogleEventMapper {

    private final WebClient webClient;

    private static final DateTimeFormatter GOOGLE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public GoogleEventMapper(WebClient commonWebClient) {
        this.webClient = commonWebClient;
    }

    public Map<String, Object> toGoogleEventBody(Event event, String token, String calendarId) {
        Map<String, Object> body = new LinkedHashMap<>();

        String timezone = event.timeZone();

        if (timezone == null || timezone.isBlank()) {
            timezone = getCalendarTimezone(token, calendarId);
        }

        body.put("summary", event.summary());
        body.put("description", event.description());

        if (event.location() != null && !event.location().isBlank()) {
            body.put("location", event.location());
        }

        body.put("start", Map.of(
                "dateTime", event.start().format(GOOGLE_DATE_TIME),
                "timeZone", timezone
        ));

        body.put("end", Map.of(
                "dateTime", event.end().format(GOOGLE_DATE_TIME),
                "timeZone", timezone
        ));

        if (event.reminderMinutesBefore() != null) {
            body.put("reminders", Map.of(
                    "useDefault", false,
                    "overrides", List.of(Map.of(
                            "method", "popup",
                            "minutes", event.reminderMinutesBefore()
                    ))
            ));
        }

        if (event.recurrenceRule() != null && !event.recurrenceRule().isBlank()) {
            body.put("recurrence", List.of(event.recurrenceRule()));
        }

        return body;
    }

    public EventListItemDto toEventListItemDto(GoogleEventItem item) {
        return new EventListItemDto(
                item.id(),
                item.summary(),
                item.description(),
                parseGoogleDateTime(item.start()),
                parseGoogleDateTime(item.end()),
                item.location(),
                null,
                item.recurrence() == null || item.recurrence().isEmpty()
                        ? null
                        : item.recurrence().get(0)
        );
    }

    private LocalDateTime parseGoogleDateTime(GoogleEventDateTime value) {
        if (value == null) return null;

        if (value.dateTime() != null) {
            return OffsetDateTime.parse(value.dateTime()).toLocalDateTime();
        }

        if (value.date() != null) {
            return LocalDate.parse(value.date()).atStartOfDay();
        }

        return null;
    }

    private String getCalendarTimezone(String token, String calendarId) {
        Map<String, Object> calendar = webClient.get()
                .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}", calendarId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Object timeZone = calendar.get("timeZone");
        return timeZone == null ? "UTC" : timeZone.toString();
    }
}