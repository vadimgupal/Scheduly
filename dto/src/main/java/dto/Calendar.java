package dto;

import java.time.ZoneId;

public record Calendar (
    String summary,
    String description,
    ZoneId timeZone
)
{}