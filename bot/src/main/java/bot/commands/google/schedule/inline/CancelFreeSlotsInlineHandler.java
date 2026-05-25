package bot.commands.google.schedule.inline;

import bot.commands.MessageHandler;
import bot.commands.google.schedule.state.FreeSlotsStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class CancelFreeSlotsInlineHandler implements MessageHandler {

    private final FreeSlotsStateStore stateStore;
    private final TelegramBot bot;

    public CancelFreeSlotsInlineHandler(
            FreeSlotsStateStore stateStore,
            TelegramBot bot
    ) {
        this.stateStore = stateStore;
        this.bot = bot;
    }

    @Override
    public String name() {
        return "Cancel free slots";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && "FREE_SLOTS:CANCEL".equals(msg.message());
    }

    @Override
    public void handle(UserMessage msg) {
        stateStore.clear(msg.chatId());

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Отменено"));
        }

        bot.execute(new SendMessage(msg.chatId(),
                "❌ Поиск свободных слотов отменён"));
    }
}