package dto;

import java.time.OffsetDateTime;

public record TaskDto(
        long id,
        String description,
        int priority,
        OffsetDateTime deadline) {
}
