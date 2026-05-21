package core.controller;

import core.DTO.GoogleEventListResponse;
import core.google.GoogleEventMapper;
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

            List<EventListItemDto> events = res.items().stream()
                    .map(eventMapper::toEventListItemDto)
                    .toList();

            log.info("[CAL_LIST] calendars loaded, count={}", res.items().size());

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
