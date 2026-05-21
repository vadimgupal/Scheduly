package bot.commands.google.event.inline;

import bot.commands.MessageHandler;
import bot.commands.google.event.service.EventFinishService;
import bot.commands.google.event.state.EventFlowMode;
import bot.commands.google.event.state.EventState;
import bot.commands.google.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SelectRecurrenceInlineHandler implements MessageHandler {

    private static final String PREFIX = "EVENT:RECURRENCE:";

    @Autowired
    private EventStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Autowired
    private EventFinishService eventFinishService;

    @Override
    public String name() {
        return "Select event recurrence";
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

        if (state != EventState.EVENT_RECURRENCE) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Сейчас нельзя выбрать повтор."));
            return;
        }

        String recurrence = msg.message().substring(PREFIX.length());

        String rrule = switch (recurrence) {
            case "NONE" -> "";
            case "DAILY" -> "RRULE:FREQ=DAILY";
            case "WEEKLY" -> "RRULE:FREQ=WEEKLY";
            case "MONTHLY" -> "RRULE:FREQ=MONTHLY";
            case "YEARLY" -> "RRULE:FREQ=YEARLY";
            default -> throw new IllegalArgumentException("Unknown recurrence: " + recurrence);
        };

        appendDraft(msg.chatId(), rrule);

        EventFlowMode mode = stateStore.getMode(msg.chatId())
                .orElseThrow(() -> new IllegalStateException("Не найден режим события"));

        if (msg.callbackQueryId() != null) {
            bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Повтор выбран"));
        }

        eventFinishService.finish(msg.chatId(), mode);
    }

    private void appendDraft(long chatId, String value) {
        String draft = stateStore.getDraft(chatId).orElse("");

        if (draft.isBlank()) {
            stateStore.putDraft(chatId, value);
        } else {
            stateStore.putDraft(chatId, draft + "\n----\n" + value);
        }
    }
}