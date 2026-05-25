package core.controller;

import core.google.GoogleEventMapper;
import core.google.GoogleTokenService;
import core.jpa.JPAServise;
import core.jpa.User;
import core.scheduler.GoogleEventFetchService;
import dto.Event;
import dto.EventListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/event")
public class EventController {
    @Qualifier("commonWebClient")
    @Autowired
    private WebClient  webClient;
    @Autowired
    private GoogleTokenService tokenService;
    @Autowired
    private GoogleEventMapper eventMapper;
    @Autowired
    private JPAServise jpaServise;
    @Autowired
    private GoogleEventFetchService googleEventFetchService;

    @PostMapping("/create")
    public ResponseEntity<String> createEvent(@RequestParam long chatId, @RequestParam String calendarId, @RequestBody Event event) {
        try {
            String token = tokenService.getAccessTokenByChatId(chatId);
            Object body = eventMapper.toGoogleEventBody(event, token, calendarId);
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
            Object body = eventMapper.toGoogleEventBody(event, token, calendarId);
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
    public ResponseEntity<List<EventListItemDto>> getEvent(
            @RequestParam long chatId,
            @RequestParam String calendarId
    ) {
        log.info("[EV_LIST] requesting events for chatId={} calendarId={}", chatId, calendarId);

        try {
            User user = jpaServise.findUserByChatId(chatId);

            if (user == null) {
                log.warn("[EV_LIST] user not found chatId={}", chatId);
                return ResponseEntity.ok(List.of());
            }

            if (user.getTimeZone() == null || user.getTimeZone().isBlank()) {
                log.warn("[EV_LIST] timezone not set chatId={}", chatId);
                return ResponseEntity.ok(List.of());
            }

            ZoneId zoneId = ZoneId.of(user.getTimeZone());

            LocalDate from = LocalDate.now(zoneId);
            LocalDate to = from.plusDays(30);

            List<EventListItemDto> events = googleEventFetchService
                    .getEventsForCalendarBetween(user, calendarId, from, to);

            log.info("[EV_LIST] events loaded chatId={} calendarId={} count={}",
                    chatId, calendarId, events.size());

            return ResponseEntity.ok(events);

        } catch (Exception e) {
            log.error("[EV_LIST] failed chatId={} calendarId={}",
                    chatId, calendarId, e);

            return ResponseEntity.status(500).body(List.of());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteEvent(
            @RequestParam long chatId,
            @RequestParam String calendarId,
            @RequestParam String eventId
    ) {
        try {
            String token = tokenService.getAccessTokenByChatId(chatId);

            webClient.delete()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}",
                            calendarId, eventId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            return ResponseEntity.ok("deleted");

        } catch (Exception e) {
            log.error("[EV_DELETE] failed chatId={} calendarId={} eventId={}",
                    chatId, calendarId, eventId, e);
            return ResponseEntity.status(500).body("server_error");
        }
    }
}
