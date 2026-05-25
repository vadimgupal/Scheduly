package core.dto;

import dto.CalendarListItemDto;

import java.util.List;

public record GoogleCalendarListResponse (
    List<CalendarListItemDto> items
){}
