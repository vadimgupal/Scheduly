package bot.commands.google.schedule.dialog;

import bot.commands.MessageHandler;
import bot.commands.google.schedule.state.FreeSlotsState;
import bot.commands.google.schedule.state.FreeSlotsStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import dto.FreeSlotDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class FreeSlotsDialogHandler implements MessageHandler {

    private final TelegramBot bot;
    private final FreeSlotsStateStore stateStore;
    private final WebClient webClient;

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm");

    public FreeSlotsDialogHandler(
            TelegramBot bot,
            FreeSlotsStateStore stateStore,
            @Qualifier("coreWebClient") WebClient webClient
    ) {
        this.bot = bot;
        this.stateStore = stateStore;
        this.webClient = webClient;
    }

    @Override
    public String name() {
        return "Free slots dialog";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        if (msg.isCallback()) return false;
        if (msg.message() == null) return false;
        if (msg.message().startsWith("/")) return false;

        return stateStore.getState(msg.chatId())
                .map(st -> st == FreeSlotsState.WAITING_PERIOD)
                .orElse(false);
    }

    @Override
    public void handle(UserMessage msg) {
        String[] parts = msg.message().trim().split("\\s+");

        if (parts.length != 2) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Нужно ввести две даты.\nНапример:\n2026-05-24 2026-05-30"));
            return;
        }

        LocalDate from;
        LocalDate to;

        try {
            from = LocalDate.parse(parts[0]);
            to = LocalDate.parse(parts[1]);
        } catch (Exception e) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Неверный формат дат.\nНапример:\n2026-05-24 2026-05-30"));
            return;
        }

        if (to.isBefore(from)) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Дата окончания не может быть раньше даты начала."));
            return;
        }

        try {
            List<FreeSlotDto> slots = webClient.get()
                    .uri(b -> b.path("/schedule/free-slots")
                            .queryParam("chatId", msg.chatId())
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .build())
                    .retrieve()
                    .bodyToFlux(FreeSlotDto.class)
                    .collectList()
                    .block();

            String timeZone = getUserTimeZone(msg.chatId());

            bot.execute(new SendMessage(
                    msg.chatId(),
                    formatSlots(slots, timeZone)
            ));

            stateStore.clear(msg.chatId());

        } catch (WebClientResponseException.BadRequest e) {
            String body = e.getResponseBodyAsString();

            if (body.contains("timezone_not_set")) {
                bot.execute(new SendMessage(msg.chatId(),
                        "Timezone не установлена.\nУстанови её командой /setTimezone"));
            } else if (body.contains("default_calendar_not_set")) {
                bot.execute(new SendMessage(msg.chatId(),
                        "Календарь по умолчанию не установлен.\nУстанови его командой /setDefaultCalendar"));
            } else {
                bot.execute(new SendMessage(msg.chatId(),
                        "Не удалось получить свободные слоты."));
            }

        } catch (Exception e) {
            log.error("[FREE_SLOTS] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось получить свободные слоты."));
        }
    }

    private String formatSlots(List<FreeSlotDto> slots, String timeZone) {
        if (slots == null || slots.isEmpty()) {
            return "Свободных слотов за этот период нет.";
        }

        ZoneId zoneId = ZoneId.of(timeZone);

        StringBuilder sb = new StringBuilder("🟢 Свободные слоты:\n\n");

        for (FreeSlotDto slot : slots) {
            ZonedDateTime start = slot.start().atZoneSameInstant(zoneId);
            ZonedDateTime end = slot.end().atZoneSameInstant(zoneId);

            sb.append("• ")
                    .append(start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                    .append(" - ")
                    .append(end.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .append("\n");
        }

        return sb.toString();
    }

    private String getUserTimeZone(long chatId) {
        try {
            String timeZone = webClient.get()
                    .uri(b -> b.path("/user/settings/timezone/get")
                            .queryParam("chatId", chatId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return timeZone == null || timeZone.isBlank() ? "UTC" : timeZone;

        } catch (Exception e) {
            return "UTC";
        }
    }
}