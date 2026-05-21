package bot.commands.task.dialog;

import bot.commands.MessageHandler;
import bot.commands.task.service.TaskFinishService;
import bot.commands.task.state.TaskFlowMode;
import bot.commands.task.state.TaskState;
import bot.commands.task.state.TaskStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@Slf4j
public class TaskDialogHandler implements MessageHandler {

    private static final String DELIMITER = "\n----\n";

    @Autowired
    private TaskStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Autowired
    private TaskFinishService taskFinishService;

    @Override
    public String name() {
        return "Task dialog handler";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        if (msg.isCallback()) return false;
        if (msg.message() == null) return false;
        if (msg.message().startsWith("/")) return false;

        return stateStore.getState(msg.chatId())
                .map(st -> st == TaskState.TASK_DESCRIPTION ||
                        st == TaskState.TASK_PRIORITY ||
                        st == TaskState.TASK_DEADLINE)
                .orElse(false);
    }

    @Override
    public void handle(UserMessage msg) {
        TaskState state = stateStore.getState(msg.chatId())
                .orElseThrow();

        TaskFlowMode mode = stateStore.getMode(msg.chatId())
                .orElseThrow();

        switch (state) {
            case TASK_DESCRIPTION -> handleDescription(msg, mode);
            case TASK_PRIORITY -> handlePriority(msg, mode);
            case TASK_DEADLINE -> handleDeadline(msg, mode);
        }
    }

    private void handleDescription(UserMessage msg, TaskFlowMode mode) {
        String value = msg.message().trim();

        if (value.isBlank()) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Описание не может быть пустым")
                    .replyMarkup(markup(mode)));
            return;
        }

        putOrAppendDraft(msg.chatId(), value);
        stateStore.putState(msg.chatId(), TaskState.TASK_PRIORITY);

        bot.execute(new SendMessage(msg.chatId(),
                "Введите приоритет от 0 до 10")
                .replyMarkup(markup(mode)));
    }

    private void handlePriority(UserMessage msg, TaskFlowMode mode) {
        Integer priority = parseIntegerOrNull(msg.message().trim());

        if (priority == null || priority < 0 || priority > 10) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Приоритет должен быть числом от 0 до 10")
                    .replyMarkup(markup(mode)));
            return;
        }

        putOrAppendDraft(msg.chatId(), String.valueOf(priority));
        stateStore.putState(msg.chatId(), TaskState.TASK_DEADLINE);

        bot.execute(new SendMessage(msg.chatId(),
                "Введите дедлайн, например: 2026-05-20T18:00:00+03:00")
                .replyMarkup(markup(mode)));
    }

    private void handleDeadline(UserMessage msg, TaskFlowMode mode) {
        String value = msg.message().trim();

        OffsetDateTime deadline;

        try {
            deadline = OffsetDateTime.parse(value);
        } catch (Exception e) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Неверный формат дедлайна.\nНапример: 2026-05-20T18:00:00+03:00")
                    .replyMarkup(markup(mode)));
            return;
        }

        putOrAppendDraft(msg.chatId(), value);
        taskFinishService.finish(msg.chatId(), mode);
    }

    private void putOrAppendDraft(long chatId, String value) {
        Optional<String> draftOpt = stateStore.getDraft(chatId);

        if (draftOpt.isEmpty()) {
            stateStore.putDraft(chatId, value);
        } else {
            stateStore.putDraft(chatId, draftOpt.get() + DELIMITER + value);
        }
    }

    private Integer parseIntegerOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private InlineKeyboardMarkup markup(TaskFlowMode mode) {
        if (mode == TaskFlowMode.CREATE) {
            return cancelMarkup();
        }

        return skipCancelMarkup();
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена").callbackData("TASK:CANCEL")
        );
    }

    private InlineKeyboardMarkup skipCancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("⏭ Пропустить").callbackData("TASK:SKIP"),
                new InlineKeyboardButton("❌ Отмена").callbackData("TASK:CANCEL")
        );
    }
}