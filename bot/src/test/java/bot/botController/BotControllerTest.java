package bot.botController;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BotControllerTest {

    private TelegramBot bot;
    private BotController controller;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);

        controller = new BotController();

        ReflectionTestUtils.setField(controller, "bot", bot);
    }

    @Test
    void notifyBotShouldSendMessage() {
        controller.notifyBot(123L, "hello");

        verify(bot).execute(any(SendMessage.class));
    }
}