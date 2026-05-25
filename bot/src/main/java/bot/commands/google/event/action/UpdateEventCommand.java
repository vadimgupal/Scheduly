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
import dto.DefaultCalendarDto;
import dto.EventListItemDto;
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

        DefaultCalendarDto calendar = getDefaultCalendar(msg.chatId());

        if (calendar != null) {
            redis.clear(msg.chatId());

            redis.putMode(msg.chatId(), EventFlowMode.UPDATE);
            redis.putTargetCalendar(msg.chatId(), calendar.id());
            redis.putState(msg.chatId(), EventState.SELECT_EVENT);

            List<EventListItemDto> events = getEvents(msg.chatId(), calendar.id());

            if (events == null || events.isEmpty()) {
                bot.execute(new SendMessage(msg.chatId(),
                        "В календаре по умолчанию \"" + calendar.summary() + "\" нет событий"));
                redis.clear(msg.chatId());
                return;
            }

            bot.execute(new SendMessage(msg.chatId(),
                    "Использую календарь по умолчанию: " + calendar.summary()
                            + "\nВыберите событие для обновления:")
                    .replyMarkup(buildEventSelectKeyboard(msg.chatId(), events)));

            return;
        }

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

    private List<EventListItemDto> getEvents(long chatId, String calendarId) {
        return webClient.get()
                .uri(b -> b.path("/event/list")
                        .queryParam("chatId", chatId)
                        .queryParam("calendarId", calendarId)
                        .build())
                .retrieve()
                .bodyToFlux(EventListItemDto.class)
                .collectList()
                .block();
    }

    private InlineKeyboardMarkup buildEventSelectKeyboard(long chatId, List<EventListItemDto> events) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

        for (int i = 0; i < events.size(); i++) {
            EventListItemDto event = events.get(i);

            redis.putOption(chatId, i, event.id());

            kb.addRow(new InlineKeyboardButton(event.name())
                    .callbackData("EVENT:SELECT_EVENT:" + i));
        }

        kb.addRow(new InlineKeyboardButton("❌ Отмена")
                .callbackData("EVENT:CANCEL"));

        return kb;
    }

    private DefaultCalendarDto getDefaultCalendar(long chatId) {
        try {
            return webClient.get()
                    .uri(b -> b.path("/calendar/default/get")
                            .queryParam("chatId", chatId)
                            .build())
                    .retrieve()
                    .bodyToMono(DefaultCalendarDto.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }
}
