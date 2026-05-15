package core.DTO;

import dto.EventListItemDto;

import java.util.List;

public record GoogleEventListResponse(
        List<EventListItemDto>  items
) {
}
