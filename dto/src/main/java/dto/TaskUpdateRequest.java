package dto;

import java.time.OffsetDateTime;

public record TaskUpdateRequest(
        String description,
        Integer priority,
        OffsetDateTime deadline
) {
}
