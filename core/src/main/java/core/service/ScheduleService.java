package core.service;

import core.jpa.JPAServise;
import core.jpa.User;
import core.scheduler.GoogleEventFetchService;
import dto.EventListItemDto;
import dto.FreeSlotDto;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScheduleService {

    private final JPAServise jpaServise;
    private final GoogleEventFetchService googleEventFetchService;

    public ScheduleService(
            JPAServise jpaServise,
            GoogleEventFetchService googleEventFetchService
    ) {
        this.jpaServise = jpaServise;
        this.googleEventFetchService = googleEventFetchService;
    }

    public List<FreeSlotDto> getFreeSlots(
            long chatId,
            LocalDate from,
            LocalDate to
    ) {
        User user = jpaServise.findUserByChatId(chatId);

        if (user.getTimeZone() == null || user.getTimeZone().isBlank()) {
            throw new IllegalStateException("timezone_not_set");
        }

        if (user.getDefaultCalendarId() == null ||
                user.getDefaultCalendarId().isBlank()) {
            throw new IllegalStateException("default_calendar_not_set");
        }

        ZoneId zoneId = ZoneId.of(user.getTimeZone());

        List<EventListItemDto> busy = googleEventFetchService
                .getEventsForUserBetween(user, from, to)
                .stream()
                .filter(e -> e.start() != null && e.end() != null)
                .sorted(Comparator.comparing(EventListItemDto::start))
                .toList();

        List<FreeSlotDto> result = new ArrayList<>();

        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {

            ZonedDateTime dayStart = day
                    .atTime(8, 0)
                    .atZone(zoneId);

            ZonedDateTime dayEnd = day
                    .atTime(22, 0)
                    .atZone(zoneId);

            ZonedDateTime cursor = dayStart;

            for (EventListItemDto event : busy) {

                ZonedDateTime eventStart = event.start().atZone(zoneId);

                ZonedDateTime eventEnd = event.end().atZone(zoneId);

                if (!eventStart.toLocalDate().equals(day)) {
                    continue;
                }

                if (eventStart.isAfter(cursor)) {

                    result.add(new FreeSlotDto(
                            cursor.toOffsetDateTime(),
                            eventStart.toOffsetDateTime()
                    ));
                }

                if (eventEnd.isAfter(cursor)) {
                    cursor = eventEnd;
                }
            }

            if (cursor.isBefore(dayEnd)) {

                result.add(new FreeSlotDto(
                        cursor.toOffsetDateTime(),
                        dayEnd.toOffsetDateTime()
                ));
            }
        }

        return result;
    }
}