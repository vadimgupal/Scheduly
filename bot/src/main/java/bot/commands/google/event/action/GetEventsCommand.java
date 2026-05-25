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

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class GetEventsCommand implements CommandHandler {
    @Autowired
    private TelegramBot bot;

    @Autowired
    private EventStateStore stateStore;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "getEvents";
    }

    @Override
    public String name() {
        return "Command for get events";
    }

    @Override
    public void handle(UserMessage msg) {
        DefaultCalendarDto calendar = getDefaultCalendar(msg.chatId());

        if (calendar != null) {
            List<EventListItemDto> events = getEvents(msg.chatId(), calendar.id());

            if (events == null || events.isEmpty()) {
                bot.execute(new SendMessage(msg.chatId(),
                        "В календаре по умолчанию \"" + calendar.summary() + "\" нет событий"));
                return;
            }

            List<EventListItemDto> eventsToShow = events.stream()
                    .limit(10)
                    .toList();

            bot.execute(new SendMessage(msg.chatId(),
                    "События из календаря по умолчанию \"" + calendar.summary() + "\":\n\n"
                            + buildStringListEvents(eventsToShow)));

            return;
        }

        log.info("[EV_VIEW] /getEvents chatId={}", msg.chatId());

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

        stateStore.putState(msg.chatId(), EventState.SELECT_CALENDAR);
        stateStore.putMode(msg.chatId(), EventFlowMode.VIEW);

        InlineKeyboardMarkup kb = buildCalendarKeyboard(msg.chatId(), calendars);

        bot.execute(new SendMessage(msg.chatId(), "Выберите календарь:").replyMarkup(kb));
    }

    private InlineKeyboardMarkup buildCalendarKeyboard (long chatId, List<CalendarListItemDto> calendars) {

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

        for (int i = 0; i < calendars.size(); i++) {

            CalendarListItemDto calendar = calendars.get(i);

            stateStore.putOption(chatId, i, calendar.id());

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

    private String buildStringListEvents(List<EventListItemDto> events) {
        StringBuilder sb = new StringBuilder();

        for (EventListItemDto event : events) {
            sb.append("📌 ").append(event.name()).append("\n");

            if (event.description() != null && !event.description().isBlank()) {
                sb.append("Описание: ").append(event.description()).append("\n");
            }

            if (event.start() != null) {
                sb.append("Дата начала: ")
                        .append(event.start().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                        .append("\n");

                sb.append("Время начала: ")
                        .append(event.start().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .append("\n");
            }

            if (event.end() != null) {
                sb.append("Дата окончания: ")
                        .append(event.end().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                        .append("\n");

                sb.append("Время окончания: ")
                        .append(event.end().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .append("\n");
            }

            if (event.location() != null && !event.location().isBlank()) {
                sb.append("Место: ").append(event.location()).append("\n");
            }

            if (event.reminderMinutesBefore() != null) {
                sb.append("Напоминание: за ")
                        .append(event.reminderMinutesBefore())
                        .append(" мин.\n");
            }

            if (event.recurrenceRule() != null && !event.recurrenceRule().isBlank()) {
                sb.append("Повтор: ")
                        .append(formatRecurrence(event.recurrenceRule()))
                        .append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatRecurrence(String rule) {
        return switch (rule) {
            case "RRULE:FREQ=DAILY" -> "каждый день";
            case "RRULE:FREQ=WEEKLY" -> "каждую неделю";
            case "RRULE:FREQ=MONTHLY" -> "каждый месяц";
            case "RRULE:FREQ=YEARLY" -> "каждый год";
            default -> rule;
        };
    }
}
