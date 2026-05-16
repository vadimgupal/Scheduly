package dto;

import java.time.OffsetDateTime;

public record TaskCreateRequest(
        String description,
        int priority,
        OffsetDateTime deadline) {
}
