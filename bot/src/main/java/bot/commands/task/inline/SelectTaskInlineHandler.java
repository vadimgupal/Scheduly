package bot.commands.task.inline;

import bot.commands.MessageHandler;
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

@Component
public class SelectTaskInlineHandler implements MessageHandler {

    private static final String PREFIX = "TASK:SELECT:";

    @Autowired
    private TaskStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Override
    public String name() {
        return "Select task for update";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && msg.message() != null
                && msg.message().startsWith(PREFIX);
    }

    @Override
    public void handle(UserMessage msg) {
        TaskState state = stateStore.getState(msg.chatId())
                .orElse(TaskState.NONE);

        if (state != TaskState.SELECT_TASK) {
            return;
        }

        int index = Integer.parseInt(msg.message().substring(PREFIX.length()));

        String taskId = stateStore.getOption(msg.chatId(), index)
                .orElseThrow(() -> new RuntimeException("Task option not found"));

        stateStore.putTargetTask(msg.chatId(), taskId);
        stateStore.putState(msg.chatId(), TaskState.TASK_DESCRIPTION);

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Задача выбрана"));
        }

        bot.execute(new SendMessage(msg.chatId(),
                "Введите новое описание задачи")
                .replyMarkup(skipCancelMarkup()));
    }

    private InlineKeyboardMarkup skipCancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("⏭ Пропустить").callbackData("TASK:SKIP"),
                new InlineKeyboardButton("❌ Отмена").callbackData("TASK:CANCEL")
        );
    }
}