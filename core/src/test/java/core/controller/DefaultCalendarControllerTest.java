package core.controller;

import core.dto.GoogleCalendarDto;
import core.google.GoogleTokenService;
import core.jpa.JPAServise;
import dto.DefaultCalendarDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultCalendarControllerTest {

    private JPAServise jpaServise;
    private GoogleTokenService tokenService;
    private MockWebServer server;
    private DefaultCalendarController controller;

    @BeforeEach
    void setUp() throws Exception {
        jpaServise = mock(JPAServise.class);
        tokenService = mock(GoogleTokenService.class);

        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();

        controller = new DefaultCalendarController();

        ReflectionTestUtils.setField(controller, "jpaServise", jpaServise);
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        ReflectionTestUtils.setField(controller, "webClient", webClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void setDefaultCalendarShouldSaveCalendar() {
        ResponseEntity<String> response = controller.setDefaultCalendar(123L, "calendar-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("saved", response.getBody());

        verify(jpaServise).setDefaultCalendar(123L, "calendar-1");
    }

    @Test
    void getDefaultCalendarShouldReturnNoContentWhenMissing() {
        when(jpaServise.getDefaultCalendar(123L))
                .thenReturn(Optional.empty());

        ResponseEntity<DefaultCalendarDto> response = controller.getDefaultCalendar(123L);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deleteDefaultCalendarShouldDeleteCalendar() {
        ResponseEntity<String> response = controller.deleteDefaultCalendar(123L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("deleted", response.getBody());

        verify(jpaServise).deleteDefaultCalendar(123L);
    }
}