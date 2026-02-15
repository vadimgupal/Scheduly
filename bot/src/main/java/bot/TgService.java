package bot;

import bot.commands.MessageHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TgService {
    private TelegramBot bot;

    List<MessageHandler> messageHandlers;

    public TgService(TelegramBot bot, List<MessageHandler> messageHandlers) {
        this.bot = bot;
        this.messageHandlers = messageHandlers;
    }

    @PostConstruct
    public void sendMessage() {
        bot.setUpdatesListener(updates -> {
            for(Update update : updates) {
                var msg = update.message();
                if (msg == null || msg.text() == null) continue;
                long chatId = msg.chat().id();
                String text = msg.text();

                String username = null;
                if(msg.from() != null) {
                    username = msg.from().username();
                }

                UserMessage userMessage = new UserMessage(chatId, username, text);
                log.info("Message '{}' from chatId={}", text, chatId);

                for (MessageHandler messageHandler : messageHandlers) {
                    if(messageHandler.shouldBeHandled(userMessage)) {
                        messageHandler.handle(userMessage);
                        break;
                    }
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;

        }, e -> {
            if (e.response() != null) {
                // got bad response from telegram
                e.response().errorCode();
                e.response().description();
            } else {
                // probably network error
                e.printStackTrace();
            }
        });
    }
}
