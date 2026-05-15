package dto;

import java.time.LocalDateTime;

public record EventListItemDto(String id,
                               String name,
                               String description,
                               LocalDateTime start,
                               LocalDateTime end,
                               String location,
                               Integer reminderMinutesBefore,
                               String recurrenceRule
) {
}
