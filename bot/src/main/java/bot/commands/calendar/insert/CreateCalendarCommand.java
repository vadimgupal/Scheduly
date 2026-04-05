package bot.commands.calendar.insert;

import bot.commands.CommandHandler;
import bot.commands.calendar.CalendarFlowMode;
import bot.commands.calendar.CalendarState;
import bot.commands.calendar.CalendarStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CreateCalendarCommand implements CommandHandler {
    @Autowired
    private CalendarStateStore redis;
    @Autowired
    private TelegramBot bot;

    @Override
    public String command() {
        return "createCalendar";
    }

    @Override
    public String name() {
        return "Command to create calendar";
    }

    @Override
    public void handle(UserMessage msg) {
        log.info("[CAL_CREATE] /createCalendar chatId={} username={}", msg.chatId(), msg.username());
        redis.putState(msg.chatId(), CalendarState.CREATE_CALENDAR_NAME);
        redis.putMode(msg.chatId(), CalendarFlowMode.CREATE);
        bot.execute(new SendMessage(msg.chatId(), "Введите название календаря").replyMarkup(cancelMarkup()));
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("CALENDAR:CANCEL")
        );
    }
}
