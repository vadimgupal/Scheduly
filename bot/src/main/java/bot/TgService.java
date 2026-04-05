package bot;

import bot.commands.MessageHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
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
                var cb = update.callbackQuery();
                if (cb != null && cb.data() != null && cb.message() != null) {
                    long chatId = cb.message().chat().id();
                    String username = cb.from() != null ? cb.from().username() : null;

                    UserMessage userMessage = UserMessage.callback(
                            chatId,
                            username,
                            cb.data(),
                            cb.id()
                    );

                    log.info("Callback '{}' from chatId={}", cb.data(), chatId);
                    dispatch(userMessage);
                    continue;
                }

                var msg = update.message();
                if (msg == null || msg.text() == null) continue;
                long chatId = msg.chat().id();
                String text = msg.text();

                String username = null;
                if(msg.from() != null) {
                    username = msg.from().username();
                }

                UserMessage userMessage = UserMessage.text(chatId, username, text);
                log.info("Message '{}' from chatId={}", text, chatId);

                dispatch(userMessage);
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

    private void dispatch(UserMessage userMessage) {
        for (MessageHandler handler : messageHandlers) {
            if (handler.shouldBeHandled(userMessage)) {
                try {
                    handler.handle(userMessage);
                } catch (Exception ex) {
                    log.error("Handler {} failed for chatId={}", handler.name(), userMessage.chatId(), ex);
                    bot.execute(new SendMessage(userMessage.chatId(), "Произошла ошибка. Попробуй ещё раз."));
                }
                break;
            }
        }
    }
}
