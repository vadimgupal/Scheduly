package bot.botController;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BotController {
    @Autowired
    private TelegramBot bot;

    @PostMapping(value = "/notify", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> notifyBot(@RequestParam long chatId, @RequestBody String text) {
        bot.execute(new SendMessage(chatId, text));
        return ResponseEntity.ok().build();
    }
}
