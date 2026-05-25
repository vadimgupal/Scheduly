package bot.commands.google.calendar.action;

import bot.commands.CommandHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import dto.DefaultCalendarDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class GetDefaultCalendarCommand implements CommandHandler {

    @Autowired
    private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String command() {
        return "getDefaultCalendar";
    }

    @Override
    public String name() {
        return "Get default calendar";
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            DefaultCalendarDto calendar = webClient.get()
                    .uri(b -> b.path("/calendar/default/get")
                            .queryParam("chatId", msg.chatId())
                            .build())
                    .retrieve()
                    .bodyToMono(DefaultCalendarDto.class)
                    .block();

            if (calendar == null) {
                bot.execute(new SendMessage(msg.chatId(),
                        "Календарь по умолчанию не установлен"));
                return;
            }

            bot.execute(new SendMessage(msg.chatId(),
                    "Календарь по умолчанию: " + calendar.summary()));

        } catch (Exception e) {
            log.error("[CAL_DEFAULT_GET] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Календарь по умолчанию не установлен"));
        }
    }
}