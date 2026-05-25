package bot.commands.settings.action;

import bot.commands.CommandHandler;
import bot.commands.settings.state.TimezoneState;
import bot.commands.settings.state.TimezoneStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SetTimezoneCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Autowired
    private TimezoneStateStore stateStore;

    @Override
    public String command() {
        return "setTimezone";
    }

    @Override
    public String name() {
        return "Set user timezone";
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.putState(msg.chatId(), TimezoneState.WAITING_TIMEZONE);

        bot.execute(new SendMessage(msg.chatId(),
                "Введите timezone.\nНапример: Europe/Athens, Europe/Moscow, Europe/Berlin")
                .replyMarkup(cancelMarkup()));
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена").callbackData("TIMEZONE:CANCEL")
        );
    }
}