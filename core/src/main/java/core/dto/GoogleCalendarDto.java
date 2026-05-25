package core.dto;

public record GoogleCalendarDto(
        String id,
        String summary,
        String timeZone
) {
}