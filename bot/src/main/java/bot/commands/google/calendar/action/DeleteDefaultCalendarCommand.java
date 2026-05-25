package bot.commands.google.calendar.action;

import bot.commands.CommandHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class DeleteDefaultCalendarCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "deleteDefaultCalendar";
    }

    @Override
    public String name() {
        return "Delete default calendar";
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            webClient.delete()
                    .uri(b -> b.path("/calendar/default/delete")
                            .queryParam("chatId", msg.chatId())
                            .build())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            bot.execute(new SendMessage(msg.chatId(),
                    "✅ Календарь по умолчанию удалён"));

        } catch (Exception e) {
            log.error("[CAL_DEFAULT_DELETE] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось удалить календарь по умолчанию"));
        }
    }
}