package bot.commands.task.inline;

import bot.commands.MessageHandler;
import bot.commands.task.state.TaskStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CancelTaskInlineHandler implements MessageHandler {

    @Autowired
    private TaskStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Override
    public String name() {
        return "Cancel task flow";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && "TASK:CANCEL".equals(msg.message());
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.clear(msg.chatId());

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Отменено"));
        }

        bot.execute(new SendMessage(msg.chatId(), "❌ Операция с задачей отменена"));
    }
}