package Commands;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartHandler implements CommandHandler<String> {
    private TelegramBot bot;

    public StartHandler(TelegramBot bot) {
        this.bot = bot;
    }

    @Override
    public void handle(long chatId, String message) {
        log.info("user {} start chat", chatId);
        bot.execute(new SendMessage(chatId, message));
    }

    @Override
    public String command() {
        return "start";
    }

    @Override
    public String validateAndConvertCommandParams(String[] commandParams) {
        return "";
    }

    @Override
    public String name() {
        return "Command to start";
    }
}
