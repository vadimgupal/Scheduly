package bot.commands.task.action;

import bot.commands.CommandHandler;
import bot.commands.task.state.TaskFlowMode;
import bot.commands.task.state.TaskState;
import bot.commands.task.state.TaskStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import dto.TaskDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@Slf4j
public class UpdateTaskCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Autowired
    private TaskStateStore stateStore;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "updateTask";
    }

    @Override
    public String name() {
        return "Command to update task";
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

            stateStore.clear(msg.chatId());
            stateStore.putMode(msg.chatId(), TaskFlowMode.UPDATE);
            stateStore.putState(msg.chatId(), TaskState.SELECT_TASK);

            bot.execute(new SendMessage(msg.chatId(),
                    "Выберите задачу для обновления:")
                    .replyMarkup(buildTaskKeyboard(msg.chatId(), tasks)));

        } catch (Exception e) {
            log.error("[TASK_UPDATE] failed to load tasks chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось получить список задач"));
        }
    }

    private InlineKeyboardMarkup buildTaskKeyboard(long chatId, List<TaskDto> tasks) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

        for (int i = 0; i < tasks.size(); i++) {
            TaskDto task = tasks.get(i);
            stateStore.putOption(chatId, i, String.valueOf(task.id()));

            kb.addRow(new InlineKeyboardButton(task.description())
                    .callbackData("TASK:SELECT:" + i));
        }

        kb.addRow(new InlineKeyboardButton("❌ Отмена")
                .callbackData("TASK:CANCEL"));

        return kb;
    }
}