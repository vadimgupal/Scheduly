package core.dto;

import java.util.List;

public record GoogleEventListResponse(
        List<GoogleEventItem>  items
) {
}
