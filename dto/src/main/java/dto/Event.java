package dto;

import java.time.LocalDateTime;

public record Event (
    String summary,
    String description,
    LocalDateTime start,
    LocalDateTime end,
    String timeZone,
    String location,
    Integer reminderMinutesBefore,
    String recurrenceRule
){}

