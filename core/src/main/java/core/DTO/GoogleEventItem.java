package core.DTO;

import java.util.List;

public record GoogleEventItem(String id,
                              String summary,
                              String description,
                              GoogleEventDateTime start,
                              GoogleEventDateTime end,
                              String location,
                              List<String> recurrence) {
}
