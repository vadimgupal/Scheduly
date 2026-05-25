package bot.commands.settings.dialog;

import bot.commands.MessageHandler;
import bot.commands.settings.state.TimezoneState;
import bot.commands.settings.state.TimezoneStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZoneId;

@Component
@Slf4j
public class TimezoneDialogHandler implements MessageHandler {

    @Autowired
    private TelegramBot bot;

    @Autowired
    private TimezoneStateStore stateStore;

    @Qualifier("coreWebClient")
    @Autowired
    private WebClient webClient;

    @Override
    public String name() {
        return "Timezone dialog handler";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        if (msg.isCallback()) return false;
        if (msg.message() == null) return false;
        if (msg.message().startsWith("/")) return false;

        return stateStore.getState(msg.chatId())
                .map(st -> st == TimezoneState.WAITING_TIMEZONE)
                .orElse(false);
    }

    @Override
    public void handle(UserMessage msg) {
        String value = msg.message().trim();

        try {
            ZoneId.of(value);
        } catch (Exception e) {
            bot.execute(new SendMessage(msg.chatId(),
                    "Некорректная timezone.\nПример: Europe/Athens"));
            return;
        }

        try {
            webClient.post()
                    .uri(b -> b.path("/user/settings/timezone/set")
                            .queryParam("chatId", msg.chatId())
                            .queryParam("timeZone", value)
                            .build())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            bot.execute(new SendMessage(msg.chatId(),
                    "✅ Timezone сохранена: " + value));

        } catch (Exception e) {
            log.error("[TZ_SET] failed chatId={} timeZone={}", msg.chatId(), value, e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось сохранить timezone"));
        } finally {
            stateStore.clear(msg.chatId());
        }
    }
}