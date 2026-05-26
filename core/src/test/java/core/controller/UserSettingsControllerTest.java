package core.controller;

import core.jpa.JPAServise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSettingsControllerTest {

    private JPAServise jpaServise;
    private UserSettingsController controller;

    @BeforeEach
    void setUp() {
        jpaServise = mock(JPAServise.class);
        controller = new UserSettingsController(jpaServise);
    }

    @Test
    void setTimeZoneShouldSaveValidTimezone() {
        ResponseEntity<String> response = controller.setTimeZone(123L, "Europe/Moscow");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("saved", response.getBody());

        verify(jpaServise).setUserTimeZone(123L, "Europe/Moscow");
    }

    @Test
    void setTimeZoneShouldReturnBadRequestForInvalidTimezone() {
        ResponseEntity<String> response = controller.setTimeZone(123L, "bad/timezone");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("invalid_timezone", response.getBody());

        verify(jpaServise, never()).setUserTimeZone(anyLong(), anyString());
    }

    @Test
    void getTimeZoneShouldReturnTimezoneWhenExists() {
        when(jpaServise.getUserTimeZone(123L))
                .thenReturn(Optional.of("Europe/Moscow"));

        ResponseEntity<String> response = controller.getTimeZone(123L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Europe/Moscow", response.getBody());
    }

    @Test
    void getTimeZoneShouldReturnNoContentWhenMissing() {
        when(jpaServise.getUserTimeZone(123L))
                .thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.getTimeZone(123L);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deleteTimeZoneShouldDeleteTimezone() {
        ResponseEntity<String> response = controller.deleteTimeZone(123L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("deleted", response.getBody());

        verify(jpaServise).deleteUserTimeZone(123L);
    }
}