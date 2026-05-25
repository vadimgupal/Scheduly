package dto;

import java.time.OffsetDateTime;

public record FreeSlotDto(
        OffsetDateTime start,
        OffsetDateTime end
) {
}