package bot.commands.calendar;

import bot.commands.CommandHandler;
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
public class UpdateCalendarCommand implements CommandHandler {
    @Autowired
    private CalendarStateStore stateStore;

    @Qualifier("coreWebClient")
    @Autowired private WebClient webClient;

    @Autowired private TelegramBot bot;

    @Override public String command() {
        return "updateCalendar";
    }

    @Override public String name() {
        return "Update calendar";
    }

    @Override
    public void handle(UserMessage msg) {
        log.info("[CAL_UPDATE] /updateCalendar chatId={}", msg.chatId());

        List<CalendarListItemDto> calendars = webClient.get()
                .uri(b->b.path("/calendar/list")
                        .queryParam("chatId", msg.chatId())
                        .build())
                .retrieve()
                .bodyToFlux(CalendarListItemDto.class)
                .collectList()
                .block();

        if (calendars == null || calendars.isEmpty()) {
            bot.execute(new SendMessage(msg.chatId(), "У тебя нет доступных календарей для обновления."));
            return;
        }

        stateStore.putState(msg.chatId(), CalendarState.UPDATE_SELECT_CALENDAR);

        InlineKeyboardMarkup kb = buildCalendarSelectKeyboard(msg.chatId(), calendars);

        var response = bot.execute(
                new SendMessage(msg.chatId(), "Выберите календарь для изменения:")
                        .replyMarkup(kb)
        );

        log.info("send update keyboard ok={}, code={}, desc={}",
                response.isOk(),
                response.errorCode(),
                response.description());
    }

    private InlineKeyboardMarkup buildCalendarSelectKeyboard(long chatId, List<CalendarListItemDto> calendars) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        for (int i = 0; i < calendars.size(); i++) {
            CalendarListItemDto calendar = calendars.get(i);

            stateStore.putOption(chatId, i, calendar.id());

            kb.addRow(
                    new InlineKeyboardButton(calendar.summary())
                            .callbackData("CALENDAR:SELECT:" + i)
            );
        }

        kb.addRow(new InlineKeyboardButton("❌ Отмена")
                .callbackData("CALENDAR:CANCEL"));

        return kb;
    }
}
