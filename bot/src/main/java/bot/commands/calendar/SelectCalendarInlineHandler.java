package bot.commands.calendar;

import bot.commands.MessageHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SelectCalendarInlineHandler implements MessageHandler {
    @Autowired private CalendarStateStore stateStore;
    @Autowired
    private TelegramBot bot;

    @Override public String name() { return "Select calendar for update"; }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && msg.message() != null
                && msg.message().startsWith("CALENDAR:SELECT:");
    }

    @Override
    public void handle(UserMessage msg) {
        var st = stateStore.getState(msg.chatId()).orElse(CalendarState.NONE);
        if (st != CalendarState.UPDATE_SELECT_CALENDAR) {
            log.warn("[CAL_UPDATE] select ignored, state={} chatId={}", st, msg.chatId());
            return;
        }

        String indexStr = msg.message().substring(16);
        int index = Integer.parseInt(indexStr);

        String calendarId = stateStore.getOption(msg.chatId(), index)
                .orElseThrow(() -> new RuntimeException("Не удалось найти календарь"));

        stateStore.putTarget(msg.chatId(), calendarId);
        stateStore.putMode(msg.chatId(), CalendarFlowMode.UPDATE);
        stateStore.putState(msg.chatId(), CalendarState.CREATE_CALENDAR_NAME);

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Выбрано"));
        }

        bot.execute(new SendMessage(msg.chatId(), "Введите новое название календаря")
                .replyMarkup(cancelMarkup()));

        log.info("[CAL_UPDATE] selected calendarId={} chatId={}", calendarId, msg.chatId());
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена").callbackData("CALENDAR:CANCEL")
        );
    }
}
