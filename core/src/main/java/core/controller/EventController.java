package core.controller;

import core.DTO.GoogleEventListResponse;
import core.google.GoogleTokenService;
import dto.Event;
import dto.EventListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/event")
public class EventController {
    @Qualifier("commonWebClient")
    @Autowired
    private WebClient  webClient;
    @Autowired
    private GoogleTokenService tokenService;

    private static final DateTimeFormatter GOOGLE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @PostMapping("/create")
    public ResponseEntity<String> createEvent(@RequestParam long chatId, @RequestParam String calendarId, @RequestBody Event event) {
        try {
            String token = tokenService.getAccessTokenByChatId(chatId);
            Object body = toGoogleEventBody(event, token, calendarId);
            webClient.post()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events", calendarId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            return ResponseEntity.status(201).body("created");
        }
        catch (WebClientResponseException e) {
            log.error("[EV_CREATE] Google error status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(500).body("server_error");
        } catch (Exception e) {
            log.error("[EV_CREATE] failed chatId={} calendarId={} event={}",
                    chatId, calendarId, event, e);
            return ResponseEntity.status(500).body("server_error");
        }
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateEvent(@RequestParam long chatId, @RequestParam String calendarId, @RequestParam String eventId, @RequestBody Event event) {
        try{
            String token = tokenService.getAccessTokenByChatId(chatId);
            Object body = toGoogleEventBody(event, token, calendarId);
            webClient.put()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}",
                            calendarId, eventId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            return ResponseEntity.status(204).body("updated");
        }
        catch (WebClientResponseException e) {
            log.error("[EV_CREATE] Google error status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(500).body("server_error");
        } catch (Exception e) {
            log.error("[EV_UPDATE] failed chatId={} calendarId={} event={}",
                    chatId, calendarId, event, e);
            return ResponseEntity.status(500).body("server_error");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<EventListItemDto>> getEvent(@RequestParam long chatId, @RequestParam String calendarId) {
        log.info("[CAL_LIST] requesting calendars for chatId={}", chatId);
        try {
            String token = tokenService.getAccessTokenByChatId(chatId);

            GoogleEventListResponse res = webClient.get()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events", calendarId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(GoogleEventListResponse.class)
                    .block();
            if (res == null || res.items() == null || res.items().isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            log.info("[CAL_LIST] calendars loaded, count={}", res.items().size());

            return ResponseEntity.ok(res.items());
        } catch (Exception e) {
            log.error("[EV_LIST] failed chatId={} calendarId={}",
                    chatId, calendarId, e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    private Map<String, Object> toGoogleEventBody(Event event, String token, String calendarId) {
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

        Map<String, Object> start = new LinkedHashMap<>();
        start.put("dateTime", event.start().format(GOOGLE_DATE_TIME));
        start.put("timeZone", timezone);

        Map<String, Object> end = new LinkedHashMap<>();
        end.put("dateTime", event.end().format(GOOGLE_DATE_TIME));
        end.put("timeZone", timezone);

        body.put("start", start);
        body.put("end", end);

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

    private String getCalendarTimezone(String token, String calendarId) {
        Map<String, Object> calendar = webClient.get()
                .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}", calendarId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Object timeZone = calendar.get("timeZone");

        if (timeZone == null) {
            return "UTC";
        }

        return timeZone.toString();
    }
}
