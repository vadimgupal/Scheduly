package bot.commands.task.service;

import bot.commands.task.state.TaskFlowMode;
import bot.commands.task.state.TaskStateStore;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import dto.TaskCreateRequest;
import dto.TaskUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class TaskFinishService {

    private static final String DELIMITER = "\n----\n";

    @Autowired
    private TaskStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    public void finish(long chatId, TaskFlowMode mode) {
        try {
            String draft = stateStore.getDraft(chatId)
                    .orElseThrow(() -> new RuntimeException("Task draft expired"));

            String[] parts = draft.split(DELIMITER, -1);

            if (parts.length != 3) {
                throw new RuntimeException("Invalid task draft parts count: " + parts.length);
            }

            if (mode == TaskFlowMode.CREATE) {
                createTask(chatId, parts);
            } else if (mode == TaskFlowMode.UPDATE) {
                updateTask(chatId, parts);
            }

        } catch (Exception e) {
            log.error("[TASK_FINISH] failed chatId={} mode={}", chatId, mode, e);
            bot.execute(new SendMessage(chatId,
                    "Не удалось сохранить задачу"));
        } finally {
            stateStore.clear(chatId);
        }
    }

    private void createTask(long chatId, String[] parts) {
        TaskCreateRequest request = new TaskCreateRequest(
                parts[0],
                Integer.parseInt(parts[1]),
                OffsetDateTime.parse(parts[2])
        );

        webClient.post()
                .uri(b -> b.path("/task/create")
                        .queryParam("chatId", chatId)
                        .build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        bot.execute(new SendMessage(chatId, "✅ Задача создана"));
    }

    private void updateTask(long chatId, String[] parts) {
        long taskId = Long.parseLong(stateStore.getTargetTask(chatId)
                .orElseThrow(() -> new RuntimeException("No target task")));

        TaskUpdateRequest request = new TaskUpdateRequest(
                parts[0].isBlank() ? null : parts[0],
                parts[1].isBlank() ? null : Integer.parseInt(parts[1]),
                parts[2].isBlank() ? null : OffsetDateTime.parse(parts[2])
        );

        webClient.put()
                .uri(b -> b.path("/task/update")
                        .queryParam("chatId", chatId)
                        .queryParam("taskId", taskId)
                        .build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        bot.execute(new SendMessage(chatId, "✅ Задача обновлена"));
    }
}