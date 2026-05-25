package core.controller;

import core.dto.GoogleCalendarDto;
import core.google.GoogleTokenService;
import core.jpa.JPAServise;
import dto.DefaultCalendarDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/calendar/default")
public class DefaultCalendarController {
    @Autowired
    private JPAServise jpaServise;

    @Autowired
    private GoogleTokenService tokenService;

    @Qualifier("commonWebClient")
    @Autowired
    private WebClient webClient;

    @PostMapping("/set")
    public ResponseEntity<String> setDefaultCalendar(
            @RequestParam long chatId,
            @RequestParam String calendarId
    ) {
        jpaServise.setDefaultCalendar(chatId, calendarId);
        return ResponseEntity.ok("saved");
    }

    @GetMapping("/get")
    public ResponseEntity<DefaultCalendarDto> getDefaultCalendar(@RequestParam long chatId) {
        try {
            Optional<String> calendarIdOpt = jpaServise.getDefaultCalendar(chatId);

            if (calendarIdOpt.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            String calendarId = calendarIdOpt.get();
            String token = tokenService.getAccessTokenByChatId(chatId);

            GoogleCalendarDto calendar = webClient.get()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}", calendarId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(GoogleCalendarDto.class)
                    .block();

            if (calendar == null) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(new DefaultCalendarDto(
                    calendarId,
                    calendar.summary(),
                    calendar.timeZone()
            ));

        } catch (Exception e) {
            log.error("[CAL_DEFAULT_GET] failed chatId={}", chatId, e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteDefaultCalendar(@RequestParam long chatId) {
        jpaServise.deleteDefaultCalendar(chatId);
        return ResponseEntity.ok("deleted");
    }
}