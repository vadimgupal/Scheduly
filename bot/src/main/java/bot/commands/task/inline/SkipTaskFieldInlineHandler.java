package bot.commands.task.inline;

import bot.commands.MessageHandler;
import bot.commands.task.service.TaskFinishService;
import bot.commands.task.state.TaskFlowMode;
import bot.commands.task.state.TaskState;
import bot.commands.task.state.TaskStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SkipTaskFieldInlineHandler implements MessageHandler {

    private static final String DELIMITER = "\n----\n";

    @Autowired
    private TaskStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Autowired
    private TaskFinishService taskFinishService;

    @Override
    public String name() {
        return "Skip task field";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && "TASK:SKIP".equals(msg.message());
    }

    @Override
    public void handle(UserMessage msg) {
        TaskFlowMode mode = stateStore.getMode(msg.chatId())
                .orElse(TaskFlowMode.CREATE);

        if (mode != TaskFlowMode.UPDATE) {
            bot.execute(new SendMessage(msg.chatId(),
                    "При создании задачи поля нельзя пропускать."));
            return;
        }

        int skips = stateStore.incrementSkipCount(msg.chatId());

        if (skips >= 3) {
            stateStore.clear(msg.chatId());

            bot.execute(new SendMessage(msg.chatId(),
                    "Вы пропустили все поля. Обновлять нечего."));
            return;
        }

        TaskState state = stateStore.getState(msg.chatId())
                .orElse(TaskState.NONE);

        appendDraft(msg.chatId(), "");

        switch (state) {
            case TASK_DESCRIPTION -> {
                stateStore.putState(msg.chatId(), TaskState.TASK_PRIORITY);
                bot.execute(new SendMessage(msg.chatId(),
                        "Введите новый приоритет от 0 до 10")
                        .replyMarkup(skipCancelMarkup()));
            }

            case TASK_PRIORITY -> {
                stateStore.putState(msg.chatId(), TaskState.TASK_DEADLINE);
                bot.execute(new SendMessage(msg.chatId(),
                        "Введите новый дедлайн, например: 2026-05-20T18:00:00+03:00")
                        .replyMarkup(skipCancelMarkup()));
            }

            case TASK_DEADLINE -> {
                taskFinishService.finish(msg.chatId(), mode);
            }

            default -> bot.execute(new SendMessage(msg.chatId(),
                    "Сейчас нечего пропускать."));
        }

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()));
        }
    }

    private void appendDraft(long chatId, String value) {
        Optional<String> draftOpt = stateStore.getDraft(chatId);

        if (draftOpt.isEmpty()) {
            stateStore.putDraft(chatId, value);
        } else {
            stateStore.putDraft(chatId, draftOpt.get() + DELIMITER + value);
        }
    }

    private InlineKeyboardMarkup skipCancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("⏭ Пропустить").callbackData("TASK:SKIP"),
                new InlineKeyboardButton("❌ Отмена").callbackData("TASK:CANCEL")
        );
    }
}