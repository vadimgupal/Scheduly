package bot.commands.settings.action;

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
public class DeleteTimezoneCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "deleteTimezone";
    }

    @Override
    public String name() {
        return "Delete user timezone";
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            webClient.delete()
                    .uri(b -> b.path("/user/settings/timezone/delete")
                            .queryParam("chatId", msg.chatId())
                            .build())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            bot.execute(new SendMessage(msg.chatId(),
                    "✅ Timezone удалена"));

        } catch (Exception e) {
            log.error("[TZ_DELETE] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось удалить timezone"));
        }
    }
}