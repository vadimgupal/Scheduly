package bot.commands.google.event.action;

import bot.commands.CommandHandler;
import bot.commands.google.event.state.EventFlowMode;
import bot.commands.google.event.state.EventState;
import bot.commands.google.event.state.EventStateStore;
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
public class UpdateEventCommand implements CommandHandler {
    @Autowired
    private TelegramBot bot;

    @Autowired
    private EventStateStore redis;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "updateEvent";
    }

    @Override
    public String name() {
        return "Command to update event";
    }

    @Override
    public void handle(UserMessage msg) {

        log.info("[EV_UPDATE] /updateEvent chatId={} username={}", msg.chatId(), msg.username());

        List<CalendarListItemDto> calendars = webClient.get()
                .uri(b -> b.path("/calendar/list")
                        .queryParam("chatId", msg.chatId())
                        .build())
                .retrieve()
                .bodyToFlux(CalendarListItemDto.class)
                .collectList()
                .block();

        if (calendars == null || calendars.isEmpty()) {
            bot.execute(new SendMessage(msg.chatId(),
                    "У тебя нет календарей. Сначала создай календарь через /createCalendar"));
            return;
        }

        redis.putState(msg.chatId(), EventState.SELECT_CALENDAR);
        redis.putMode(msg.chatId(), EventFlowMode.UPDATE);

        InlineKeyboardMarkup kb = buildCalendarKeyboard(msg.chatId(), calendars);

        bot.execute(new SendMessage(msg.chatId(),
                "Выберите календарь для обновления события:")
                .replyMarkup(kb));
    }

    private InlineKeyboardMarkup buildCalendarKeyboard(long chatId,
                                                       List<CalendarListItemDto> calendars) {

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

        for (int i = 0; i < calendars.size(); i++) {

            CalendarListItemDto calendar = calendars.get(i);

            redis.putOption(chatId, i, calendar.id());

            kb.addRow(
                    new InlineKeyboardButton(calendar.summary())
                            .callbackData("EVENT:SELECT_CALENDAR:" + i)
            );
        }

        kb.addRow(
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("EVENT:CANCEL")
        );

        return kb;
    }
}
