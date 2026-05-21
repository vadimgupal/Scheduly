package bot.commands.google.event.inline;

import bot.commands.MessageHandler;
import bot.commands.google.event.state.EventStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class DeleteEventInlineHandler implements MessageHandler {

    private static final String PREFIX = "EVENT:DELETE_EVENT:";

    @Autowired
    private EventStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String name() {
        return "Delete event";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && msg.message() != null
                && msg.message().startsWith(PREFIX);
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            int index = Integer.parseInt(msg.message().substring(PREFIX.length()));

            String eventId = stateStore.getOption(msg.chatId(), index)
                    .orElseThrow(() -> new RuntimeException("Не удалось найти событие"));

            String calendarId = stateStore.getTargetCalendar(msg.chatId())
                    .orElseThrow(() -> new RuntimeException("Не удалось найти календарь"));

            webClient.delete()
                    .uri(b -> b.path("/event/delete")
                            .queryParam("chatId", msg.chatId())
                            .queryParam("calendarId", calendarId)
                            .queryParam("eventId", eventId)
                            .build())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            if (msg.callbackQueryId() != null) {
                bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Удалено"));
            }

            bot.execute(new SendMessage(msg.chatId(), "✅ Событие удалено"));

        } catch (Exception e) {
            log.error("[EV_DELETE] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось удалить событие"));
        } finally {
            stateStore.clear(msg.chatId());
        }
    }
}