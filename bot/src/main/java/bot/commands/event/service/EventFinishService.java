package bot.commands.event.service;

import bot.commands.event.state.EventFlowMode;
import bot.commands.event.state.EventStateStore;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import dto.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Slf4j
@Component
public class EventFinishService {

    @Autowired
    private EventStateStore stateStore;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Autowired
    private TelegramBot bot;

    public void finish(long chatId, EventFlowMode mode) {
        try {
            String draft = stateStore.getDraft(chatId)
                    .orElseThrow(() -> new RuntimeException("Диалог истёк"));

            String[] parts = draft.split("\n----\n", -1);

            if (parts.length != 8) {
                throw new RuntimeException("Некорректное количество полей: " + parts.length);
            }

            Event event = new Event(
                    parts[0],
                    parts[1],
                    LocalDateTime.parse(parts[2]),
                    LocalDateTime.parse(parts[3]),
                    emptyToNull(parts[4]),
                    emptyToNull(parts[5]),
                    parts[6].isBlank() ? null : Integer.parseInt(parts[6]),
                    emptyToNull(parts[7])
            );

            String calendarId = stateStore.getTargetCalendar(chatId)
                    .orElseThrow(() -> new RuntimeException("Нет календаря"));

            if (mode == EventFlowMode.CREATE) {
                webClient.post()
                        .uri(b -> b.path("/event/create")
                                .queryParam("chatId", chatId)
                                .queryParam("calendarId", calendarId)
                                .build())
                        .bodyValue(event)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

                bot.execute(new SendMessage(chatId, "✅ Событие создано"));
            } else if (mode == EventFlowMode.UPDATE) {
                String eventId = stateStore.getTargetEvent(chatId)
                        .orElseThrow(() -> new RuntimeException("Нет события"));

                webClient.put()
                        .uri(b -> b.path("/events/update")
                                .queryParam("chatId", chatId)
                                .queryParam("calendarId", calendarId)
                                .queryParam("eventId", eventId)
                                .build())
                        .bodyValue(event)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

                bot.execute(new SendMessage(chatId, "✅ Событие обновлено"));
            }

        } catch (Exception e) {
            log.error("[EV_FINISH] failed to save event chatId={} mode={}", chatId, mode, e);
            bot.execute(new SendMessage(chatId,
                    "Не удалось сохранить событие. Попробуй позже."));
        } finally {
            stateStore.clear(chatId);
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}