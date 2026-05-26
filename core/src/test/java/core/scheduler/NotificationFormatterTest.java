package core.scheduler;

import core.jpa.Task;
import dto.EventListItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationFormatterTest {

    private NotificationFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new NotificationFormatter();
    }

    @Test
    void eventOneHourReminderShouldFormatMessage() {
        EventListItemDto event = new EventListItemDto(
                "event-1",
                "meeting",
                null,
                LocalDateTime.of(2026, 5, 25, 14, 0),
                LocalDateTime.of(2026, 5, 25, 15, 0),
                null,
                null,
                null
        );

        String result = formatter.eventOneHourReminder(event);

        assertTrue(result.contains("Через час событие"));
        assertTrue(result.contains("meeting"));
        assertTrue(result.contains("25.05.2026 14:00"));
    }

    @Test
    void taskOneHourReminderShouldFormatMessage() {
        Task task = new Task();
        task.setDescription("finish project");

        String result = formatter.taskOneHourReminder(
                task,
                OffsetDateTime.of(2026, 5, 25, 14, 0, 0, 0, ZoneOffset.ofHours(3)).toZonedDateTime()
        );

        assertTrue(result.contains("Через час дедлайн задачи"));
        assertTrue(result.contains("finish project"));
        assertTrue(result.contains("25.05.2026 14:00"));
    }

    @Test
    void dailySummaryShouldIncludeEventsAndTasks() {
        EventListItemDto event = new EventListItemDto(
                "event-1",
                "meeting",
                null,
                LocalDateTime.of(2026, 5, 25, 14, 0),
                LocalDateTime.of(2026, 5, 25, 15, 0),
                null,
                null,
                null
        );

        Task task = new Task();
        task.setDescription("task");
        task.setPriority(3);
        task.setDeadline(OffsetDateTime.of(2026, 5, 25, 18, 0, 0, 0, ZoneOffset.ofHours(3)));

        String result = formatter.dailySummary(
                LocalDate.of(2026, 5, 25),
                List.of(event),
                List.of(task)
        );

        assertTrue(result.contains("Сводка на сегодня"));
        assertTrue(result.contains("meeting"));
        assertTrue(result.contains("task"));
        assertTrue(result.contains("Приоритет: 3"));
    }

    @Test
    void weeklySummaryShouldShowEmptyLists() {
        String result = formatter.weeklySummary(
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 31),
                List.of(),
                List.of()
        );

        assertTrue(result.contains("Сводка на неделю"));
        assertTrue(result.contains("Нет событий"));
        assertTrue(result.contains("Нет задач"));
    }
}