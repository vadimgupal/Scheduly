package core.DTO;

import java.util.List;

public record GoogleEventListResponse(
        List<GoogleEventItem>  items
) {
}
