package bot.commands.google.schedule.action;

import bot.commands.CommandHandler;
import bot.commands.google.schedule.state.FreeSlotsState;
import bot.commands.google.schedule.state.FreeSlotsStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class FreeSlotsCommand implements CommandHandler {

    private final TelegramBot bot;
    private final FreeSlotsStateStore stateStore;

    public FreeSlotsCommand(TelegramBot bot, FreeSlotsStateStore stateStore) {
        this.bot = bot;
        this.stateStore = stateStore;
    }

    @Override
    public String command() {
        return "freeSlots";
    }

    @Override
    public String name() {
        return "Get free slots";
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.putState(msg.chatId(), FreeSlotsState.WAITING_PERIOD);

        bot.execute(new SendMessage(msg.chatId(),
                "Введите период дат.\nНапример:\n2026-05-24 2026-05-30")
                .replyMarkup(cancelMarkup()));
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("FREE_SLOTS:CANCEL")
        );
    }
}