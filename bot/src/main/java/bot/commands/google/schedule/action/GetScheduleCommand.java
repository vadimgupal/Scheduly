package bot.commands.google.schedule.action;

import bot.commands.CommandHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import dto.DefaultCalendarDto;
import dto.EventListItemDto;
import dto.TaskDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class GetScheduleCommand implements CommandHandler {

    private final TelegramBot bot;
    private final WebClient webClient;

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public GetScheduleCommand(
            TelegramBot bot,
            @Qualifier("coreWebClient") WebClient webClient
    ) {
        this.bot = bot;
        this.webClient = webClient;
    }

    @Override
    public String command() {
        return "getSchedule";
    }

    @Override
    public String name() {
        return "Get events and tasks";
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            DefaultCalendarDto calendar = getDefaultCalendar(msg.chatId());
            List<TaskDto> tasks = getTasks(msg.chatId());

            String message = buildMessage(msg, calendar, tasks);

            bot.execute(new SendMessage(msg.chatId(), message));

        } catch (Exception e) {
            log.error("[GET_SCHEDULE] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось получить расписание"));
        }
    }

    private DefaultCalendarDto getDefaultCalendar(long chatId) {
        try {
            return webClient.get()
                    .uri(b -> b.path("/calendar/default/get")
                            .queryParam("chatId", chatId)
                            .build())
                    .retrieve()
                    .bodyToMono(DefaultCalendarDto.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    private List<EventListItemDto> getEvents(long chatId, String calendarId) {
        return webClient.get()
                .uri(b -> b.path("/event/list")
                        .queryParam("chatId", chatId)
                        .queryParam("calendarId", calendarId)
                        .build())
                .retrieve()
                .bodyToFlux(EventListItemDto.class)
                .collectList()
                .block();
    }

    private List<TaskDto> getTasks(long chatId) {
        return webClient.get()
                .uri(b -> b.path("/task/list")
                        .queryParam("chatId", chatId)
                        .build())
                .retrieve()
                .bodyToFlux(TaskDto.class)
                .collectList()
                .block();
    }

    private void appendEvent(StringBuilder sb, EventListItemDto event) {
        sb.append("• ").append(event.name()).append("\n");

        if (event.description() != null && !event.description().isBlank()) {
            sb.append("  Описание: ").append(event.description()).append("\n");
        }

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

        if (event.location() != null && !event.location().isBlank()) {
            sb.append("  Место: ").append(event.location()).append("\n");
        }

        sb.append("\n");
    }

    private void appendTask(StringBuilder sb, TaskDto task) {
        sb.append("• ").append(task.description()).append("\n");
        sb.append("  Приоритет: ").append(task.priority()).append("\n");

        if (task.deadline() != null) {
            sb.append("  Дедлайн: ")
                    .append(formatDeadline(task.deadline()))
                    .append("\n");
        }

        sb.append("\n");
    }

    private String formatDeadline(OffsetDateTime deadline) {
        return deadline.format(DATE_TIME);
    }

    private String buildMessage(UserMessage msg, DefaultCalendarDto calendar, List<TaskDto> tasks) {
        StringBuilder sb = new StringBuilder();

        sb.append("📌 Расписание\n\n");

        sb.append("📅 События:\n");

        if (calendar == null) {
            sb.append("Календарь по умолчанию не установлен.\n");
        } else {
            List<EventListItemDto> events = getEvents(msg.chatId(), calendar.id());

            if (events == null || events.isEmpty()) {
                sb.append("Нет событий.\n");
            } else {
                for (EventListItemDto event : events) {
                    appendEvent(sb, event);
                }
            }
        }

        sb.append("\n✅ Задачи:\n");

        if (tasks == null || tasks.isEmpty()) {
            sb.append("Нет задач.\n");
        } else {
            for (TaskDto task : tasks) {
                appendTask(sb, task);
            }
        }

        return  sb.toString();
    }
}