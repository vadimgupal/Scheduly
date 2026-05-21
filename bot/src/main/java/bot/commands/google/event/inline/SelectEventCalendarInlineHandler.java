package bot.commands.google.event.inline;

import bot.commands.MessageHandler;
import bot.commands.google.event.state.EventFlowMode;
import bot.commands.google.event.state.EventState;
import bot.commands.google.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import dto.EventListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@Slf4j
public class SelectEventCalendarInlineHandler implements MessageHandler {

    private static final String PREFIX = "EVENT:SELECT_CALENDAR:";

    @Autowired
    private EventStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    public WebClient webClient;

    @Override
    public String name() {
        return "Select calendar for event";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && msg.message() != null
                && msg.message().startsWith(PREFIX);
    }

    @Override
    public void handle(UserMessage msg) {
        EventState state = stateStore.getState(msg.chatId())
                .orElse(EventState.NONE);

        if (state != EventState.SELECT_CALENDAR) {
            log.warn("[EV] select calendar ignored, state={} chatId={}",
                    state, msg.chatId());
            return;
        }

        try {
            String indexStr = msg.message().substring(PREFIX.length());
            int index = Integer.parseInt(indexStr);

            String calendarId = stateStore.getOption(msg.chatId(), index)
                    .orElseThrow(() -> new RuntimeException("Не удалось найти календарь"));

            stateStore.putTargetCalendar(msg.chatId(), calendarId);

            EventFlowMode mode = stateStore.getMode(msg.chatId())
                    .orElseThrow();

            if(mode == EventFlowMode.CREATE) {
                stateStore.putState(msg.chatId(), EventState.EVENT_SUMMARY);
                bot.execute(new SendMessage(msg.chatId(), "Введите название события").replyMarkup(cancelMarkup()));
            } else if (mode == EventFlowMode.UPDATE) {
                stateStore.putState(msg.chatId(), EventState.SELECT_EVENT);

                List<EventListItemDto> events = getEvents(msg, calendarId);

                if (events == null || events.isEmpty()) {
                    bot.execute(new SendMessage(msg.chatId(), "В этом календаре нет событий"));
                    stateStore.clear(msg.chatId());
                    return;
                }

                InlineKeyboardMarkup kb = buildEventSelectKeyboard(msg, events);

                bot.execute(new SendMessage(msg.chatId(), "Выберите событие:")
                        .replyMarkup(kb));
            } else if (mode == EventFlowMode.VIEW) {
                List<EventListItemDto> events = getEvents(msg, calendarId);

                if (events == null || events.isEmpty()) {
                    bot.execute(new SendMessage(msg.chatId(), "В этом календаре нет событий"));
                    stateStore.clear(msg.chatId());
                    return;
                }

                bot.execute(new SendMessage(msg.chatId(), buildStringListEvents(events)));

                stateStore.clear(msg.chatId());
            } else if (mode == EventFlowMode.DELETE) {
                stateStore.putState(msg.chatId(), EventState.SELECT_EVENT);

                List<EventListItemDto> events = getEvents(msg, calendarId);

                if (events == null || events.isEmpty()) {
                    bot.execute(new SendMessage(msg.chatId(), "В этом календаре нет событий"));
                    stateStore.clear(msg.chatId());
                    return;
                }

                InlineKeyboardMarkup kb = buildEventDeleteKeyboard(msg, events);

                bot.execute(new SendMessage(msg.chatId(),
                        "Выберите событие для удаления:")
                        .replyMarkup(kb));
            }

            if (msg.callbackQueryId() != null) {
                bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Календарь выбран"));
            }

            log.info("[EV] selected calendarId={} chatId={}",
                    calendarId, msg.chatId());

        } catch (Exception e) {
            log.error("[EV] select calendar error chatId={}", msg.chatId(), e);

            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось выбрать календарь. Попробуй начать заново"));

            stateStore.clear(msg.chatId());
        }
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("EVENT:CANCEL")
        );
    }

    private InlineKeyboardMarkup buildEventSelectKeyboard(UserMessage msg, List<EventListItemDto> events) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

        for (int i = 0; i < events.size(); i++) {
            EventListItemDto event = events.get(i);

            stateStore.putOption(msg.chatId(), i, event.id());

            kb.addRow(
                    new InlineKeyboardButton(event.name())
                            .callbackData("EVENT:SELECT_EVENT:" + i)
            );
        }

        kb.addRow(
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("EVENT:CANCEL")
        );
        return kb;
    }

    private List<EventListItemDto> getEvents(UserMessage msg, String calendarId) {
        List<EventListItemDto> events = webClient.get()
                .uri(b -> b.path("/event/list")
                        .queryParam("chatId", msg.chatId())
                        .queryParam("calendarId", calendarId)
                        .build())
                .retrieve()
                .bodyToFlux(EventListItemDto.class)
                .collectList()
                .block();
        return events;
    }

    private String buildStringListEvents(List<EventListItemDto> events) {
        StringBuilder sb = new StringBuilder("Список ваших событий для выбранного календаря:\n\n");

        for (EventListItemDto e : events) {
            sb.append("• ").append(e.name())
                    .append("\n")
                    .append(e.start())
                    .append(" - ")
                    .append(e.end())
                    .append("\n\n");
        }

        return sb.toString();
    }

    private InlineKeyboardMarkup buildEventDeleteKeyboard(UserMessage msg, List<EventListItemDto> events) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

        for (int i = 0; i < events.size(); i++) {
            EventListItemDto event = events.get(i);

            stateStore.putOption(msg.chatId(), i, event.id());

            kb.addRow(new InlineKeyboardButton(event.name())
                    .callbackData("EVENT:DELETE_EVENT:" + i));
        }

        kb.addRow(new InlineKeyboardButton("❌ Отмена")
                .callbackData("EVENT:CANCEL"));

        return kb;
    }
}