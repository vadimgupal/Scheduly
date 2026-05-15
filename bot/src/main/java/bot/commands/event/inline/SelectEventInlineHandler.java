package bot.commands.event.inline;

import bot.commands.MessageHandler;
import bot.commands.event.state.EventState;
import bot.commands.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SelectEventInlineHandler implements MessageHandler {

    private static final String PREFIX = "EVENT:SELECT_EVENT:";

    @Autowired
    private EventStateStore stateStore;
    @Autowired private TelegramBot bot;

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && msg.message() != null
                && msg.message().startsWith(PREFIX);
    }

    @Override
    public String name() {
        return "Select event for update";
    }

    @Override
    public void handle(UserMessage msg) {
        EventState state = stateStore.getState(msg.chatId())
                .orElse(EventState.NONE);

        if (state != EventState.SELECT_EVENT) {
            log.warn("[EV_UPDATE] select event ignored, state={}", state);
            return;
        }

        String indexStr = msg.message().substring(PREFIX.length());
        int index = Integer.parseInt(indexStr);

        String eventId = stateStore.getOption(msg.chatId(), index)
                .orElseThrow(() -> new RuntimeException("Не удалось найти событие"));

        stateStore.putTargetEvent(msg.chatId(), eventId);

        stateStore.putState(msg.chatId(), EventState.EVENT_SUMMARY);

        bot.execute(new SendMessage(msg.chatId(),
                "Введите новое название события"));

        log.info("[EV_UPDATE] selected eventId={}", eventId);
    }
}