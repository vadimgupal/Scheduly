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
public class GetTimezoneCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "getTimezone";
    }

    @Override
    public String name() {
        return "Get user timezone";
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            String timeZone = webClient.get()
                    .uri(b -> b.path("/user/settings/timezone/get")
                            .queryParam("chatId", msg.chatId())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (timeZone == null || timeZone.isBlank()) {
                bot.execute(new SendMessage(msg.chatId(),
                        "Timezone не установлена"));
                return;
            }

            bot.execute(new SendMessage(msg.chatId(),
                    "Текущая timezone: " + timeZone));

        } catch (Exception e) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Timezone не установлена"));
        }
    }
}