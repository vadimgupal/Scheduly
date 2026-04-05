package bot.commands.calendar;

import bot.commands.MessageHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CancelCalendarInlineHandler implements MessageHandler {

    @Autowired
    private CalendarStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback() && "CALENDAR:CANCEL".equals(msg.message());
    }

    @Override
    public String name() {
        return "Cancel create calendar";
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.clear(msg.chatId());

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Отменено"));
        }

        bot.execute(new SendMessage(msg.chatId(), "❌ Создание календаря отменено"));
    }
}