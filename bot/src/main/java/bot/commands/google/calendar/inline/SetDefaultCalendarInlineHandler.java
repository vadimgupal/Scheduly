package bot.commands.google.calendar.inline;

import bot.commands.MessageHandler;
import bot.commands.google.calendar.state.CalendarStateStore;
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
public class SetDefaultCalendarInlineHandler implements MessageHandler {

    private static final String PREFIX = "CALENDAR:DEFAULT:SET:";

    @Autowired
    private CalendarStateStore stateStore;

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String name() {
        return "Set default calendar inline";
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

            String calendarId = stateStore.getOption(msg.chatId(), index)
                    .orElseThrow(() -> new RuntimeException("Calendar option not found"));

            webClient.post()
                    .uri(b -> b.path("/calendar/default/set")
                            .queryParam("chatId", msg.chatId())
                            .queryParam("calendarId", calendarId)
                            .build())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            if (msg.callbackQueryId() != null) {
                bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Сохранено"));
            }

            bot.execute(new SendMessage(msg.chatId(),
                    "✅ Календарь по умолчанию установлен"));

        } catch (Exception e) {
            log.error("[CAL_DEFAULT_SET] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось установить календарь по умолчанию"));
        }
    }
}