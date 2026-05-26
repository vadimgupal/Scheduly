package core.controller;

import core.google.GoogleEventMapper;
import core.google.GoogleTokenService;
import core.jpa.JPAServise;
import core.jpa.User;
import core.scheduler.GoogleEventFetchService;
import dto.EventListItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventControllerTest {

    private GoogleTokenService tokenService;
    private GoogleEventMapper eventMapper;
    private JPAServise jpaServise;
    private GoogleEventFetchService googleEventFetchService;
    private EventController controller;

    @BeforeEach
    void setUp() {
        tokenService = mock(GoogleTokenService.class);
        eventMapper = mock(GoogleEventMapper.class);
        jpaServise = mock(JPAServise.class);
        googleEventFetchService = mock(GoogleEventFetchService.class);

        controller = new EventController();

        ReflectionTestUtils.setField(controller, "webClient", WebClient.builder().build());
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        ReflectionTestUtils.setField(controller, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(controller, "jpaServise", jpaServise);
        ReflectionTestUtils.setField(controller, "googleEventFetchService", googleEventFetchService);
    }

    @Test
    void getEventShouldReturnEmptyListWhenUserIsNull() {
        when(jpaServise.findUserByChatId(123L))
                .thenReturn(null);

        ResponseEntity<List<EventListItemDto>> response =
                controller.getEvent(123L, "calendar-1");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getEventShouldReturnEmptyListWhenTimezoneMissing() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);
        user.setTimeZone(null);

        when(jpaServise.findUserByChatId(123L))
                .thenReturn(user);

        ResponseEntity<List<EventListItemDto>> response =
                controller.getEvent(123L, "calendar-1");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());

        verifyNoInteractions(googleEventFetchService);
    }

    @Test
    void getEventShouldReturnEvents() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);
        user.setTimeZone("Europe/Moscow");

        EventListItemDto event = new EventListItemDto(
                "event-1",
                "event",
                "description",
                LocalDateTime.of(2026, 5, 25, 14, 0),
                LocalDateTime.of(2026, 5, 25, 15, 0),
                "office",
                60,
                null
        );

        when(jpaServise.findUserByChatId(123L))
                .thenReturn(user);

        when(googleEventFetchService.getEventsForCalendarBetween(
                eq(user),
                eq("calendar-1"),
                any(),
                any()
        )).thenReturn(List.of(event));

        ResponseEntity<List<EventListItemDto>> response =
                controller.getEvent(123L, "calendar-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("event", response.getBody().getFirst().name());
    }

    @Test
    void getEventShouldReturn500OnException() {
        when(jpaServise.findUserByChatId(123L))
                .thenThrow(new RuntimeException("error"));

        ResponseEntity<List<EventListItemDto>> response =
                controller.getEvent(123L, "calendar-1");

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }
}