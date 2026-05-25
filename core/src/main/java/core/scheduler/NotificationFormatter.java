package core.scheduler;

import core.jpa.Task;
import dto.EventListItemDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NotificationFormatter {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String taskOneHourReminder(Task task, ZonedDateTime deadline) {
        return "⏰ Через час дедлайн задачи:\n\n" +
                "• " + task.getDescription() + "\n" +
                "Дедлайн: " + deadline.format(DATE_TIME);
    }

    public String eventOneHourReminder(EventListItemDto event) {
        return "⏰ Через час событие:\n\n" +
                "• " + event.name() + "\n" +
                "Начало: " + event.start().format(DATE_TIME);
    }

    public String eventCustomReminder(EventListItemDto event) {
        return "⏰ Напоминание о событии:\n\n" +
                "• " + event.name() + "\n" +
                "Начало: " + event.start().format(DATE_TIME);
    }

    public String weeklySummary(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<EventListItemDto> events,
            List<Task> tasks
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("📅 Сводка на неделю\n")
                .append(weekStart.format(DATE))
                .append(" - ")
                .append(weekEnd.format(DATE))
                .append("\n\n");

        appendEvents(sb, events);
        sb.append("\n");
        appendTasks(sb, tasks);

        return sb.toString();
    }

    public String dailySummary(
            LocalDate today,
            List<EventListItemDto> events,
            List<Task> tasks
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("📌 Сводка на сегодня ")
                .append(today.format(DATE))
                .append("\n\n");

        appendEvents(sb, events);
        sb.append("\n");
        appendTasks(sb, tasks);

        return sb.toString();
    }

    private void appendEvents(StringBuilder sb, List<EventListItemDto> events) {
        sb.append("События:\n");

        if (events == null || events.isEmpty()) {
            sb.append("Нет событий\n");
            return;
        }

        for (EventListItemDto event : events) {
            sb.append("• ").append(event.name()).append("\n");

            if (event.start() != null) {
                sb.append("  Начало: ")
                        .append(event.start().format(DATE_TIME))
                        .append("\n");
            }

            if (event.end() != null) {
                sb.append("  Конец: ")
                        .append(event.end().format(DATE_TIME))
                        .append("\n");
            }
        }
    }

    private void appendTasks(StringBuilder sb, List<Task> tasks) {
        sb.append("Задачи:\n");

        if (tasks == null || tasks.isEmpty()) {
            sb.append("Нет задач\n");
            return;
        }

        for (Task task : tasks) {
            sb.append("• ").append(task.getDescription()).append("\n")
                    .append("  Приоритет: ").append(task.getPriority()).append("\n");

            if (task.getDeadline() != null) {
                sb.append("  Дедлайн: ")
                        .append(task.getDeadline().format(DATE_TIME))
                        .append("\n");
            }
        }
    }
}