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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateTaskCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Autowired
    private TaskStateStore stateStore;

    @Override
    public String command() {
        return "createTask";
    }

    @Override
    public String name() {
        return "Command to create task";
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.clear(msg.chatId());

        stateStore.putMode(msg.chatId(), TaskFlowMode.CREATE);
        stateStore.putState(msg.chatId(), TaskState.TASK_DESCRIPTION);

        bot.execute(new SendMessage(msg.chatId(),
                "Введите описание задачи")
                .replyMarkup(cancelMarkup()));
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена").callbackData("TASK:CANCEL")
        );
    }
}