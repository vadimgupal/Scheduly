package core.dto;

import java.util.List;
import java.util.Map;

public record GoogleEventItem(String id,
                              String status,
                              String summary,
                              String description,
                              GoogleEventDateTime start,
                              GoogleEventDateTime end,
                              String location,
                              Map<String, Object> reminders,
                              List<String> recurrence) {
}
