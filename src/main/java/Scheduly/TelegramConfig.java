package backend.academy.hotelroomreservation;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramConfig {
    @Autowired
    private BotConfig config;

    @Bean
    public TelegramBot getTelegramBot() {
        return new TelegramBot(config.telegramToken());
    }
}
