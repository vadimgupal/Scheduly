package core.controller;

import core.service.ScheduleService;
import dto.FreeSlotDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/free-slots")
    public ResponseEntity<List<FreeSlotDto>> getFreeSlots(
            @RequestParam long chatId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return ResponseEntity.ok(
                scheduleService.getFreeSlots(
                        chatId,
                        LocalDate.parse(from),
                        LocalDate.parse(to)
                )
        );
    }
}