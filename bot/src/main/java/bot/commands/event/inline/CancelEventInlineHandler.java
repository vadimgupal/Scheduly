package bot.commands.event.inline;

import bot.commands.MessageHandler;
import bot.commands.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CancelEventInlineHandler implements MessageHandler {
    @Autowired
    private EventStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback() && "EVENT:CANCEL".equals(msg.message());
    }

    @Override
    public String name() {
        return "Cancel create event";
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.clear(msg.chatId());

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Отменено"));
        }

        bot.execute(new SendMessage(msg.chatId(), "❌ Создание события отменено"));
    }
}
