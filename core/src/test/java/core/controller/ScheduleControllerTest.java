package core.controller;

import core.service.ScheduleService;
import dto.FreeSlotDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleControllerTest {

    private ScheduleService scheduleService;
    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        scheduleService = mock(ScheduleService.class);
        controller = new ScheduleController(scheduleService);
    }

    @Test
    void getFreeSlotsShouldReturnSlots() {
        FreeSlotDto slot = new FreeSlotDto(
                OffsetDateTime.parse("2026-05-25T08:00:00+03:00"),
                OffsetDateTime.parse("2026-05-25T14:00:00+03:00")
        );

        when(scheduleService.getFreeSlots(
                eq(123L),
                any(),
                any()
        )).thenReturn(List.of(slot));

        ResponseEntity<List<FreeSlotDto>> response =
                controller.getFreeSlots(123L, "2026-05-25", "2026-05-25");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());

        verify(scheduleService).getFreeSlots(
                eq(123L),
                eq(java.time.LocalDate.of(2026, 5, 25)),
                eq(java.time.LocalDate.of(2026, 5, 25))
        );
    }
}