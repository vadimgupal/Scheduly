package bot.commands.event.inline;

import bot.commands.MessageHandler;
import bot.commands.event.service.EventFinishService;
import bot.commands.event.state.EventState;
import bot.commands.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SkipEventFieldInlineHandler implements MessageHandler {

    @Autowired
    private EventStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Autowired
    private EventFinishService eventFinishService;

    @Override
    public String name() {
        return "Skip optional event field";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && "EVENT:SKIP".equals(msg.message());
    }

    @Override
    public void handle(UserMessage msg) {
        EventState state = stateStore.getState(msg.chatId())
                .orElse(EventState.NONE);

        switch (state) {
            case EVENT_TIMEZONE -> {
                appendDraft(msg.chatId(), "");
                stateStore.putState(msg.chatId(), EventState.EVENT_LOCATION);

                bot.execute(new SendMessage(msg.chatId(),
                        "Введите место проведения:")
                        .replyMarkup(optionalMarkup()));
            }

            case EVENT_LOCATION -> {
                appendDraft(msg.chatId(), "");
                stateStore.putState(msg.chatId(), EventState.EVENT_REMINDER);

                bot.execute(new SendMessage(msg.chatId(),
                        "За сколько минут напомнить?")
                        .replyMarkup(optionalMarkup()));
            }

            case EVENT_REMINDER -> {
                appendDraft(msg.chatId(), "");
                stateStore.putState(msg.chatId(), EventState.EVENT_RECURRENCE);

                bot.execute(new SendMessage(msg.chatId(),
                        "Выберите повтор события:")
                        .replyMarkup(recurrenceMarkup()));
            }

            default -> bot.execute(new SendMessage(msg.chatId(),
                    "Это поле нельзя пропустить."));
        }

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()));
        }
    }

    private void appendDraft(long chatId, String value) {
        String draft = stateStore.getDraft(chatId).orElse("");

        if (draft.isBlank()) {
            stateStore.putDraft(chatId, value);
        } else {
            stateStore.putDraft(chatId, draft + "\n----\n" + value);
        }
    }

    private InlineKeyboardMarkup optionalMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("⏭ Пропустить").callbackData("EVENT:SKIP"),
                new InlineKeyboardButton("❌ Отмена").callbackData("EVENT:CANCEL")
        );
    }

    private InlineKeyboardMarkup recurrenceMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("Без повтора")
                        .callbackData("EVENT:RECURRENCE:NONE"),
                new InlineKeyboardButton("Каждый день")
                        .callbackData("EVENT:RECURRENCE:DAILY"),
                new InlineKeyboardButton("Каждую неделю")
                        .callbackData("EVENT:RECURRENCE:WEEKLY"),
                new InlineKeyboardButton("Каждый месяц")
                        .callbackData("EVENT:RECURRENCE:MONTHLY"),
                new InlineKeyboardButton("Каждый год")
                        .callbackData("EVENT:RECURRENCE:YEARLY"),
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("EVENT:CANCEL")
        );
    }
}