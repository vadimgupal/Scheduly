package core.controller;

import core.DTO.GoogleCalendarListResponse;
import core.google.GoogleTokenService;
import dto.Calendar;
import dto.CalendarListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/calendar")
public class CalendarController {
    @Qualifier("commonWebClient")
    @Autowired
    private WebClient webClient;
    @Autowired
    private GoogleTokenService tokenService;

    @PostMapping("/create")
    public ResponseEntity<String> createCalendar(@RequestParam long chatId, @RequestBody Calendar calendar) {
        try {
            String token = tokenService.getAccessTokenByChatId(chatId);
            webClient.post()
                    .uri("https://www.googleapis.com/calendar/v3/calendars?access_token=" + token)
                    .bodyValue(calendar)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            return ResponseEntity.status(201).body("created");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("server_error");
        }
    }

    @GetMapping("list")
    public ResponseEntity<List<CalendarListItemDto>> listCalendars(@RequestParam long chatId) {
        log.info("[CAL_LIST] requesting calendars for chatId={}", chatId);
        try {
            String token = tokenService.getAccessTokenByChatId(chatId);

            GoogleCalendarListResponse res = webClient.get()
                    .uri("https://www.googleapis.com/calendar/v3/users/me/calendarList")
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(GoogleCalendarListResponse.class)
                    .block();
            if (res == null || res.items() == null || res.items().isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            log.info("[CAL_LIST] calendars loaded, count={}", res.items().size());

            return ResponseEntity.ok(res.items());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateCalendar(@RequestParam long chatId, @RequestParam String calendarId, @RequestBody Calendar calendar) {
        try {
            String token = tokenService.getAccessTokenByChatId(chatId);
            webClient.put()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/" + calendarId)
                    .headers(h -> h.setBearerAuth(token))
                    .bodyValue(calendar)
                    .retrieve()
                    .bodyToMono(Calendar.class)
                    .block();
            return ResponseEntity.status(204).body("updated");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("server_error");
        }
    }
}
