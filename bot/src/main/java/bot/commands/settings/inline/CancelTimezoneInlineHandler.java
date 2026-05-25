package bot.commands.settings.inline;

import bot.commands.MessageHandler;
import bot.commands.settings.state.TimezoneStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CancelTimezoneInlineHandler implements MessageHandler {

    @Autowired
    private TimezoneStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Override
    public String name() {
        return "Cancel timezone";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && "TIMEZONE:CANCEL".equals(msg.message());
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.clear(msg.chatId());

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Отменено"));
        }

        bot.execute(new SendMessage(msg.chatId(),
                "❌ Настройка timezone отменена"));
    }
}