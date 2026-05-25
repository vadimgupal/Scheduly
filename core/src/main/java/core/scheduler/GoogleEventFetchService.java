package core.scheduler;

import core.dto.GoogleEventListResponse;
import core.google.GoogleEventMapper;
import core.google.GoogleTokenService;
import core.jpa.User;
import dto.EventListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class GoogleEventFetchService {

    private final WebClient webClient;
    private final GoogleTokenService tokenService;
    private final GoogleEventMapper eventMapper;

    public GoogleEventFetchService(
            @Qualifier("commonWebClient") WebClient webClient,
            GoogleTokenService tokenService,
            GoogleEventMapper eventMapper
    ) {
        this.webClient = webClient;
        this.tokenService = tokenService;
        this.eventMapper = eventMapper;
    }

    public List<EventListItemDto> getEventsForCalendarBetween(
            User user,
            String calendarId,
            LocalDate from,
            LocalDate to
    ) {
        if (calendarId == null || calendarId.isBlank()) {
            log.info("[GOOGLE_EVENTS] skip userId={} chatId={} reason=no_calendar_id",
                    user.getId(), user.getChatId());

            return List.of();
        }

        if (user.getTimeZone() == null || user.getTimeZone().isBlank()) {
            log.info("[GOOGLE_EVENTS] skip userId={} chatId={} reason=no_timezone",
                    user.getId(), user.getChatId());

            return List.of();
        }

        String token = tokenService.getAccessTokenByUserId(user.getId());

        ZoneId zoneId = ZoneId.of(user.getTimeZone());

        String timeMin = from
                .atStartOfDay(zoneId)
                .toInstant()
                .toString();

        String timeMax = to
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toString();

        log.info("[GOOGLE_EVENTS] userId={} chatId={} calendarId={} from={} to={} timeMin={} timeMax={}",
                user.getId(),
                user.getChatId(),
                calendarId,
                from,
                to,
                timeMin,
                timeMax);

        try {
            GoogleEventListResponse res = webClient.get()
                    .uri(b -> b
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/calendar/v3/calendars/{calendarId}/events")
                            .queryParam("timeMin", timeMin)
                            .queryParam("timeMax", timeMax)
                            .queryParam("singleEvents", true)
                            .queryParam("orderBy", "startTime")
                            .queryParam("showDeleted", false)
                            .queryParam("timeZone", zoneId.toString())
                            .build(calendarId))
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(GoogleEventListResponse.class)
                    .block();

            if (res == null || res.items() == null) {
                log.info("[GOOGLE_EVENTS] calendarId={} events count=0", calendarId);
                return List.of();
            }

            res.items().forEach(e -> log.info(
                    "[GOOGLE_EVENT_RAW] id={} recurringEventId={} status={} summary={} start={} end={}",
                    e.id(),
                    e.recurrence(),
                    e.status(),
                    e.summary(),
                    e.start(),
                    e.end()
            ));
            List<EventListItemDto> events = res.items().stream()
                    .filter(e -> e.status() == null || !"cancelled".equals(e.status()))
                    .map(eventMapper::toEventListItemDto)
                    .filter(e -> e.start() != null)
                    .filter(e -> e.end() != null)
                    .toList();

            log.info("[GOOGLE_EVENTS] calendarId={} events count={}",
                    calendarId, events.size());

            return events;

        } catch (WebClientResponseException e) {
            log.warn("[GOOGLE_EVENTS] failed userId={} calendarId={} status={} timeMin={} timeMax={} body={}",
                    user.getId(),
                    calendarId,
                    e.getStatusCode(),
                    timeMin,
                    timeMax,
                    e.getResponseBodyAsString());

            return List.of();

        } catch (Exception e) {
            log.warn("[GOOGLE_EVENTS] failed userId={} calendarId={}",
                    user.getId(), calendarId, e);

            return List.of();
        }
    }

    public List<EventListItemDto> getEventsForUserBetween(
            User user,
            LocalDate from,
            LocalDate to
    ) {
        if (user.getDefaultCalendarId() == null || user.getDefaultCalendarId().isBlank()) {
            log.info("[GOOGLE_EVENTS] skip userId={} chatId={} reason=no_default_calendar",
                    user.getId(), user.getChatId());

            return List.of();
        }

        return getEventsForCalendarBetween(
                user,
                user.getDefaultCalendarId(),
                from,
                to
        );
    }
}