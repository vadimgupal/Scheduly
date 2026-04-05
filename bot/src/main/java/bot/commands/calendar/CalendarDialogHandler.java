package bot.commands.calendar;

import bot.commands.MessageHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import dto.Calendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

@Component
@Slf4j
public class CalendarDialogHandler implements MessageHandler {
    @Autowired
    private CalendarStateStore stateStore;
    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;
    @Autowired
    private TelegramBot bot;

    @Override
    public String name() {
        return "Calendar Handler";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        if(msg.message().startsWith("/")) return false;
        if (msg.isCallback()) return false;

        return stateStore.getState(msg.chatId())
                .map(st -> st== CalendarState.CREATE_CALENDAR_NAME||
                        st==CalendarState.CREATE_CALENDAR_DESCRIPTION ||
                        st==CalendarState.CREATE_CALENDAR_TIMEZONE)
                .orElse(false);
    }

    @Override
    public void handle(UserMessage msg) {
        CalendarState state = stateStore.getState(msg.chatId())
                .orElseThrow(() -> new RuntimeException("Некорректное состояние системы"));

        CalendarFlowMode mode = stateStore.getMode(msg.chatId()).orElse(CalendarFlowMode.CREATE);

        log.info("[CAL_FLOW] get state={} mode={} chatId={}", state, mode, msg.chatId());

        switch (state) {
            case CREATE_CALENDAR_NAME -> {
                stateStore.putState(msg.chatId(), CalendarState.CREATE_CALENDAR_DESCRIPTION);
                stateStore.putDraft(msg.chatId(), msg.message());
                String s;
                if(mode.equals(CalendarFlowMode.CREATE)) {
                    s = "Введите описание календаря";
                } else {
                    s = "Введите новое описание календаря";
                }
                bot.execute(new SendMessage(msg.chatId(), s).replyMarkup(cancelMarkup()));
                log.info("[CAL_FLOW] step=NAME saved, next=DESCRIPTION chatId={}", msg.chatId());
            }
            case CREATE_CALENDAR_DESCRIPTION -> {
                stateStore.putState(msg.chatId(), CalendarState.CREATE_CALENDAR_TIMEZONE);
                String message = msg.message();
                String restartHint = (mode == CalendarFlowMode.CREATE) ? "/createCalendar" : "/updateCalendar";
                String draft = stateStore.getDraft(msg.chatId())
                        .orElseThrow(() -> new RuntimeException("Диалог истёк. Начни заново: " + restartHint));
                stateStore.putDraft(msg.chatId(), draft + "\n----\n" + message);
                String s;
                if(mode.equals(CalendarFlowMode.CREATE)) {
                    s = "Введите timezone (например: UTC+2 или Europe/Berlin)";
                } else {
                    s = "Введите новую timezone (например: UTC+2 или Europe/Berlin)";
                }
                bot.execute(new SendMessage(msg.chatId(), s).replyMarkup(cancelMarkup()));
                log.info("[CAL_FLOW] step=DESCRIPTION saved, next=TIMEZONE chatId={} draftLenBefore={}",
                        msg.chatId(), draft.length());
            }
            case CREATE_CALENDAR_TIMEZONE -> {
                try {
                    String tzInput = msg.message();
                    String restartHint = (mode == CalendarFlowMode.CREATE) ? "/createCalendar" : "/updateCalendar";

                    String draft = stateStore.getDraft(msg.chatId())
                            .orElseThrow(() -> new RuntimeException("Диалог истёк. Начни заново: "+ restartHint));

                    String[] s = draft.split("\n----\n");
                    if (s.length != 2) throw new RuntimeException("Диалог истек. Начни заново: "+ restartHint);
                    ZoneId zoneId;
                    try {
                        // сначала пробуем как нормальную зону (Europe/Berlin)
                        zoneId = ZoneId.of(tzInput.trim());
                    } catch (Exception ignore) {
                        // иначе как UTC+2
                        zoneId = ZoneId.of(toGoogleCalendarTimeZone(tzInput));
                    }

                    Calendar calendar = new Calendar(s[0], s[1], zoneId);

                    log.info("[CAL_FLOW] sending to core chatId={} name='{}' descLen={} zoneId={}",
                            msg.chatId(), s[0], s[1].length(), zoneId);

                    if(mode.equals(CalendarFlowMode.CREATE)) {
                        webClient.post()
                                .uri(uribuilder -> uribuilder
                                        .path("/calendar/create")
                                        .queryParam("chatId", msg.chatId())
                                        .build()
                                )
                                .bodyValue(calendar)
                                .retrieve()
                                .onStatus(st -> st.is4xxClientError() || st.is5xxServerError(),
                                        resp -> resp.bodyToMono(String.class)
                                                .defaultIfEmpty("<empty body>")
                                                .flatMap(body -> Mono.error(new RuntimeException(
                                                        "core error: " + resp.statusCode() + ", body=" + body
                                                )))
                                )
                                .bodyToMono(Void.class)
                                .block();
                    } else {
                        String calendarId = stateStore.getTarget(msg.chatId())
                                .orElseThrow(() -> new RuntimeException("Не выбран календарь для обновления"));
                        webClient.put()
                                .uri(uribuilder -> uribuilder
                                        .path("/calendar/update")
                                        .queryParam("chatId", msg.chatId())
                                        .queryParam("calendarId", calendarId)
                                        .build()
                                )
                                .bodyValue(calendar)
                                .retrieve()
                                .onStatus(st -> st.is4xxClientError() || st.is5xxServerError(),
                                        resp -> resp.bodyToMono(String.class)
                                                .defaultIfEmpty("<empty body>")
                                                .flatMap(body -> Mono.error(new RuntimeException(
                                                        "core error: " + resp.statusCode() + ", body=" + body
                                                )))
                                )
                                .bodyToMono(Void.class)
                                .block();
                    }
                    String okText = (mode == CalendarFlowMode.CREATE)
                            ? "✅ Календарь создан!"
                            : "✅ Календарь обновлён!";
                    bot.execute(new SendMessage(msg.chatId(), okText));
                    log.info("[CAL_FLOW] done chatId={}", msg.chatId());
                } catch (IllegalArgumentException ex) {
                    log.warn("[CAL_FLOW] bad timezone chatId={} text='{}' err={}",
                            msg.chatId(), msg.message(), ex.getMessage());
                    bot.execute(new SendMessage(msg.chatId(),
                            "Неверный timezone. Введите timezone в формате: UTC+2 или Europe/Berlin").replyMarkup(cancelMarkup()));
                    // оставь state TIMEZONE, пусть вводит ещё раз
                    stateStore.putState(msg.chatId(), CalendarState.CREATE_CALENDAR_TIMEZONE);
                    return;
                } catch (Exception ex) {
                    log.error("[CAL_FLOW] failed chatId={} err={}",
                            msg.chatId(), ex.toString(), ex);
                    String s;
                    if(mode.equals(CalendarFlowMode.CREATE)) {
                        s="создать";
                    } else {
                        s = "обновить";
                    }
                    bot.execute(new SendMessage(msg.chatId(),
                            "Не получилось " + s + " календарь. Попробуй позже или начни заново"));
                    stateStore.clear(msg.chatId());
                    return;
                }

                stateStore.clear(msg.chatId());
            }
        }
    }

    private static String toGoogleCalendarTimeZone(String input) {
        String s = input.trim().toUpperCase();
        if (s.startsWith("UTC")) s = s.substring(3);
        if (!s.matches("[+-]\\d{1,2}")) throw new IllegalArgumentException("Неправильный формат времени");
        int offset = Integer.parseInt(s);
        if (offset < -12 || offset > 14) throw new IllegalArgumentException("Такого времени не существует");
        int inverted = -offset;
        return "Etc/GMT" + (inverted >= 0 ? "+" + inverted : inverted);
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("CALENDAR:CANCEL")
        );
    }
}
