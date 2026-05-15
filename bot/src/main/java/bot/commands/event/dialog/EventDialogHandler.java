package bot.commands.event.dialog;

import bot.commands.MessageHandler;
import bot.commands.event.state.EventFlowMode;
import bot.commands.event.state.EventState;
import bot.commands.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Slf4j
public class EventDialogHandler implements MessageHandler {

    private static final String DELIMITER = "\n----\n";

    @Autowired
    private EventStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Override
    public String name() {
        return "Event Handler";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {

        if (msg.isCallback()) return false;
        if (msg.message() == null) return false;
        if (msg.message().startsWith("/")) return false;

        return stateStore.getState(msg.chatId())
                .map(st ->
                        st == EventState.EVENT_SUMMARY ||
                                st == EventState.EVENT_DESCRIPTION ||
                                st == EventState.EVENT_START_TIME ||
                                st == EventState.EVENT_END_TIME ||
                                st == EventState.EVENT_TIMEZONE ||
                                st == EventState.EVENT_LOCATION ||
                                st == EventState.EVENT_REMINDER
                )
                .orElse(false);
    }

    @Override
    public void handle(UserMessage msg) {

        EventState state = stateStore.getState(msg.chatId())
                .orElseThrow(() -> new RuntimeException("Некорректное состояние"));

        EventFlowMode mode = stateStore.getMode(msg.chatId())
                .orElseThrow(() -> new RuntimeException("Не найден mode"));

        log.info("[EV_FLOW] state={} mode={} chatId={}",
                state, mode, msg.chatId());

        switch (state) {

            case EVENT_SUMMARY -> handleSummary(msg);

            case EVENT_DESCRIPTION -> handleDescription(msg);

            case EVENT_START_TIME -> handleStartTime(msg);

            case EVENT_END_TIME -> handleEndTime(msg);

            case EVENT_TIMEZONE -> handleTimezone(msg);

            case EVENT_LOCATION -> handleLocation(msg);

            case EVENT_REMINDER -> handleReminder(msg);
        }
    }

    private void handleSummary(UserMessage msg) {

        String value = msg.message().trim();

        if (value.isBlank()) {
            bot.execute(new SendMessage(
                    msg.chatId(),
                    "Название события не может быть пустым."
            ).replyMarkup(cancelMarkup()));

            return;
        }

        stateStore.putDraft(msg.chatId(), value);

        stateStore.putState(msg.chatId(),
                EventState.EVENT_DESCRIPTION);

        bot.execute(new SendMessage(
                msg.chatId(),
                "Введите описание события"
        ).replyMarkup(cancelMarkup()));
    }

    private void handleDescription(UserMessage msg) {

        String value = msg.message().trim();

        if (value.isBlank()) {
            bot.execute(new SendMessage(
                    msg.chatId(),
                    "Описание события не может быть пустым."
            ).replyMarkup(cancelMarkup()));

            return;
        }

        appendDraft(msg.chatId(), value);

        stateStore.putState(msg.chatId(),
                EventState.EVENT_START_TIME);

        bot.execute(new SendMessage(
                msg.chatId(),
                "Введите дату и время начала.\nНапример: 2026-04-10T14:00"
        ).replyMarkup(cancelMarkup()));
    }

    private void handleStartTime(UserMessage msg) {

        String value = msg.message().trim();

        LocalDateTime start = parseDateTimeOrNull(value);

        if (start == null) {

            bot.execute(new SendMessage(
                    msg.chatId(),
                    "Неверный формат даты начала.\nНапример: 2026-04-10T14:00"
            ).replyMarkup(cancelMarkup()));

            return;
        }

        appendDraft(msg.chatId(), value);

        stateStore.putState(msg.chatId(),
                EventState.EVENT_END_TIME);

        bot.execute(new SendMessage(
                msg.chatId(),
                "Введите дату и время окончания.\nНапример: 2026-04-10T15:00"
        ).replyMarkup(cancelMarkup()));
    }

    private void handleEndTime(UserMessage msg) {

        String value = msg.message().trim();

        LocalDateTime end = parseDateTimeOrNull(value);

        if (end == null) {

            bot.execute(new SendMessage(
                    msg.chatId(),
                    "Неверный формат даты окончания.\nНапример: 2026-04-10T15:00"
            ).replyMarkup(cancelMarkup()));

            return;
        }

        String draft = stateStore.getDraft(msg.chatId())
                .orElseThrow(() -> new RuntimeException("Диалог истёк"));

        String[] parts = draft.split(DELIMITER, -1);

        LocalDateTime start = LocalDateTime.parse(parts[2]);

        if (!end.isAfter(start)) {

            bot.execute(new SendMessage(
                    msg.chatId(),
                    "Дата окончания должна быть позже даты начала."
            ).replyMarkup(cancelMarkup()));

            return;
        }

        appendDraft(msg.chatId(), value);

        stateStore.putState(msg.chatId(),
                EventState.EVENT_TIMEZONE);

        bot.execute(new SendMessage(
                msg.chatId(),
                "Введите timezone.\nНапример: Europe/Berlin"
        ).replyMarkup(optionalMarkup()));
    }

    private void handleTimezone(UserMessage msg) {

        String value = msg.message().trim();

        try {
            ZoneId.of(value);
        } catch (Exception e) {

            bot.execute(new SendMessage(
                    msg.chatId(),
                    "Некорректная timezone.\nНапример: Europe/Berlin"
            ).replyMarkup(optionalMarkup()));

            return;
        }

        appendDraft(msg.chatId(), value);

        stateStore.putState(msg.chatId(),
                EventState.EVENT_LOCATION);

        bot.execute(new SendMessage(
                msg.chatId(),
                "Введите место проведения"
        ).replyMarkup(optionalMarkup()));
    }

    private void handleLocation(UserMessage msg) {

        String value = msg.message().trim();

        appendDraft(msg.chatId(), value);

        stateStore.putState(msg.chatId(),
                EventState.EVENT_REMINDER);

        bot.execute(new SendMessage(
                msg.chatId(),
                "За сколько минут напомнить?\nНапример: 10"
        ).replyMarkup(optionalMarkup()));
    }

    private void handleReminder(UserMessage msg) {

        String value = msg.message().trim();

        Integer reminder = parseIntegerOrNull(value);

        if (reminder == null || reminder < 0) {

            bot.execute(new SendMessage(
                    msg.chatId(),
                    "Напоминание должно быть неотрицательным числом.\nНапример: 10"
            ).replyMarkup(optionalMarkup()));

            return;
        }

        appendDraft(msg.chatId(), value);

        stateStore.putState(msg.chatId(),
                EventState.EVENT_RECURRENCE);

        bot.execute(new SendMessage(
                msg.chatId(),
                "Выберите повтор события:"
        ).replyMarkup(recurrenceMarkup()));
    }

    private void appendDraft(long chatId, String value) {
        String draft = stateStore.getDraft(chatId)
                .orElse("");

        if (draft.isBlank()) {
            stateStore.putDraft(chatId, value);
        } else {
            stateStore.putDraft(chatId, draft + DELIMITER + value);
        }
    }

    private LocalDateTime parseDateTimeOrNull(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntegerOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private InlineKeyboardMarkup cancelMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("EVENT:CANCEL")
        );
    }

    private InlineKeyboardMarkup optionalMarkup() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("⏭ Пропустить")
                        .callbackData("EVENT:SKIP"),
                new InlineKeyboardButton("❌ Отмена")
                        .callbackData("EVENT:CANCEL")
        );
    }

    private InlineKeyboardMarkup recurrenceMarkup() {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();

        kb.addRow(new InlineKeyboardButton("Без повтора")
                .callbackData("EVENT:RECURRENCE:NONE"));

        kb.addRow(new InlineKeyboardButton("Каждый день")
                .callbackData("EVENT:RECURRENCE:DAILY"));

        kb.addRow(new InlineKeyboardButton("Каждую неделю")
                .callbackData("EVENT:RECURRENCE:WEEKLY"));

        kb.addRow(new InlineKeyboardButton("Каждый месяц")
                .callbackData("EVENT:RECURRENCE:MONTHLY"));

        kb.addRow(new InlineKeyboardButton("Каждый год")
                .callbackData("EVENT:RECURRENCE:YEARLY"));

        kb.addRow(new InlineKeyboardButton("❌ Отмена")
                .callbackData("EVENT:CANCEL"));

        return kb;
    }
}