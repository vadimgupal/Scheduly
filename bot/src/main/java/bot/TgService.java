package bot;

import bot.Commands.MessageHandler;
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

    List<MessageHandler<?>> messageHandlers;

    public TgService(TelegramBot bot, List<MessageHandler<?>> messageHandlers) {
        this.bot = bot;
        this.messageHandlers = messageHandlers;
    }

    @PostConstruct
    public void sendMessage() {
        bot.setUpdatesListener(updates -> {
            for(Update update : updates) {
                long chatId = update.message().chat().id();
                String message = update.message().text();
                log.info("Message received: " + message + " from " + chatId);
                for(MessageHandler<?> messageHandler : messageHandlers) {
                    if(messageHandler.shouldBeHandled(message)) {
                        messageHandler.handle(chatId, message);
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
