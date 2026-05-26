package core.service;

import core.jpa.JPAServise;
import core.jpa.User;
import core.scheduler.GoogleEventFetchService;
import dto.EventListItemDto;
import dto.FreeSlotDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleServiceTest {

    private JPAServise jpaServise;
    private GoogleEventFetchService googleEventFetchService;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        jpaServise = mock(JPAServise.class);
        googleEventFetchService = mock(GoogleEventFetchService.class);

        service = new ScheduleService(jpaServise, googleEventFetchService);
    }

    @Test
    void getFreeSlotsShouldThrowWhenTimezoneMissing() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);
        user.setTimeZone(null);
        user.setDefaultCalendarId("calendar-1");

        when(jpaServise.findUserByChatId(123L))
                .thenReturn(user);

        assertThrows(IllegalStateException.class,
                () -> service.getFreeSlots(
                        123L,
                        LocalDate.of(2026, 5, 25),
                        LocalDate.of(2026, 5, 25)
                ));
    }

    @Test
    void getFreeSlotsShouldThrowWhenDefaultCalendarMissing() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);
        user.setTimeZone("Europe/Moscow");
        user.setDefaultCalendarId(null);

        when(jpaServise.findUserByChatId(123L))
                .thenReturn(user);

        assertThrows(IllegalStateException.class,
                () -> service.getFreeSlots(
                        123L,
                        LocalDate.of(2026, 5, 25),
                        LocalDate.of(2026, 5, 25)
                ));
    }

    @Test
    void getFreeSlotsShouldReturnWholeDayWhenNoEvents() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);
        user.setTimeZone("Europe/Moscow");
        user.setDefaultCalendarId("calendar-1");

        when(jpaServise.findUserByChatId(123L))
                .thenReturn(user);
        when(googleEventFetchService.getEventsForUserBetween(
                user,
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 25)
        )).thenReturn(List.of());

        List<FreeSlotDto> result = service.getFreeSlots(
                123L,
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 25)
        );

        assertEquals(1, result.size());
        assertEquals("2026-05-25T08:00+03:00", result.getFirst().start().toString());
        assertEquals("2026-05-25T22:00+03:00", result.getFirst().end().toString());
    }

    @Test
    void getFreeSlotsShouldSplitDayByBusyEvent() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);
        user.setTimeZone("Europe/Moscow");
        user.setDefaultCalendarId("calendar-1");

        EventListItemDto event = new EventListItemDto(
                "event-1",
                "meeting",
                null,
                LocalDateTime.of(2026, 5, 25, 14, 0),
                LocalDateTime.of(2026, 5, 25, 15, 0),
                null,
                null,
                null
        );

        when(jpaServise.findUserByChatId(123L))
                .thenReturn(user);
        when(googleEventFetchService.getEventsForUserBetween(
                user,
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 25)
        )).thenReturn(List.of(event));

        List<FreeSlotDto> result = service.getFreeSlots(
                123L,
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 25)
        );

        assertEquals(2, result.size());

        assertEquals("2026-05-25T08:00+03:00", result.get(0).start().toString());
        assertEquals("2026-05-25T14:00+03:00", result.get(0).end().toString());

        assertEquals("2026-05-25T15:00+03:00", result.get(1).start().toString());
        assertEquals("2026-05-25T22:00+03:00", result.get(1).end().toString());
    }
}