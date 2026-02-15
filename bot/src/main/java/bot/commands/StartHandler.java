package bot.commands;

import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class StartHandler implements CommandHandler {
    @Autowired
    private TelegramBot bot;
    @Autowired
    private WebClient webClient;

    @Override
    public void handle(UserMessage msg) {
        log.info("user {} start chat, username={}", msg.chatId(), msg.username());

        String url = webClient.post()
                .uri(
                        UriComponentsBuilder
                                .fromUriString("http://localhost:8081/auth")
                                .queryParam("chatId", msg.chatId())
                                .queryParam("username", msg.username())
                                .toUriString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Авторизироваться в Google").url(url));
        bot.execute(new SendMessage(msg.chatId(),
                msg.message()).replyMarkup(inlineKeyboardMarkup));
    }

    @Override
    public String command() {
        return "start";
    }

    @Override
    public String name() {
        return "Command to start";
    }
}
