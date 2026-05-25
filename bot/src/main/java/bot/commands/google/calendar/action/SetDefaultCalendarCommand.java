package bot.commands.google.calendar.action;

import bot.commands.CommandHandler;
import bot.commands.google.calendar.state.CalendarStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import dto.CalendarListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@Slf4j
public class SetDefaultCalendarCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Autowired
    private CalendarStateStore stateStore;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "setDefaultCalendar";
    }

    @Override
    public String name() {
        return "Set default calendar";
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            List<CalendarListItemDto> calendars = webClient.get()
                    .uri(b -> b.path("/calendar/list")
                            .queryParam("chatId", msg.chatId())
                            .build())
                    .retrieve()
                    .bodyToFlux(CalendarListItemDto.class)
                    .collectList()
                    .block();

            if (calendars == null || calendars.isEmpty()) {
                bot.execute(new SendMessage(msg.chatId(), "У тебя нет календарей"));
                return;
            }

            InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

            for (int i = 0; i < calendars.size(); i++) {
                CalendarListItemDto calendar = calendars.get(i);

                stateStore.putOption(msg.chatId(), i, calendar.id());

                kb.addRow(new InlineKeyboardButton(calendar.summary())
                        .callbackData("CALENDAR:DEFAULT:SET:" + i));
            }

            kb.addRow(new InlineKeyboardButton("❌ Отмена")
                    .callbackData("CALENDAR:CANCEL"));

            bot.execute(new SendMessage(msg.chatId(),
                    "Выберите календарь по умолчанию:")
                    .replyMarkup(kb));

        } catch (Exception e) {
            log.error("[CAL_DEFAULT_SET] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось получить список календарей"));
        }
    }
}