package bot.commands.task.action;

import bot.commands.CommandHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import dto.TaskDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class GetTasksCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "getTasks";
    }

    @Override
    public String name() {
        return "Command to get tasks";
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            List<TaskDto> tasks = webClient.get()
                    .uri(b -> b.path("/task/list")
                            .queryParam("chatId", msg.chatId())
                            .build())
                    .retrieve()
                    .bodyToFlux(TaskDto.class)
                    .collectList()
                    .block();

            if (tasks == null || tasks.isEmpty()) {
                bot.execute(new SendMessage(msg.chatId(), "У тебя пока нет задач"));
                return;
            }
            String timeZone = getUserTimeZone(msg.chatId());
            bot.execute(new SendMessage(msg.chatId(), buildTasksMessage(tasks, timeZone)));
        } catch (Exception e) {
            log.error("[TASK_LIST] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось получить список задач"));
        }
    }

    private String buildTasksMessage(List<TaskDto> tasks, String timeZone) {
        StringBuilder sb = new StringBuilder("📋 Твои задачи:\n\n");

        for (TaskDto task : tasks) {
            sb.append("ID: ").append(task.id()).append("\n")
                    .append("• ").append(task.description()).append("\n")
                    .append("Приоритет: ").append(task.priority()).append("\n")
                    .append("Дедлайн: ").append(formatDeadline(task.deadline(), timeZone)).append("\n\n");
        }

        return sb.toString();
    }

    private String getUserTimeZone(long chatId) {
        try {
            String timeZone = webClient.get()
                    .uri(b -> b.path("/user/settings/timezone/get")
                            .queryParam("chatId", chatId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (timeZone == null || timeZone.isBlank()) {
                return "UTC";
            }

            return timeZone;

        } catch (Exception e) {
            return "UTC";
        }
    }

    private String formatDeadline(OffsetDateTime deadline, String timeZone) {
        if (deadline == null) {
            return "не указан";
        }

        return deadline
                .atZoneSameInstant(ZoneId.of(timeZone))
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}