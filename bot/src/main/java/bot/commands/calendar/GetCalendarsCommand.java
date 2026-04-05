package bot.commands.calendar;

import bot.commands.CommandHandler;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import dto.CalendarListItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class GetCalendarsCommand implements CommandHandler {
    @Autowired
    @Qualifier("coreWebClient")
    private WebClient webClient;
    @Autowired
    private TelegramBot bot;

    @Override
    public String command() {
        return "getCalendars";
    }

    @Override
    public String name() {
        return "get calendars";
    }

    @Override
    public void handle(UserMessage msg) {
        List<CalendarListItemDto> calendars = webClient.get()
                .uri(b->b.path("/calendar/list")
                        .queryParam("chatId", msg.chatId())
                        .build())
                .retrieve()
                .bodyToFlux(CalendarListItemDto.class)
                .collectList()
                .block();
        if(calendars == null || calendars.isEmpty()) {
            bot.execute(new SendMessage(msg.chatId(), "Список календарей пуст"));
            return;
        }
        bot.execute(new SendMessage(msg.chatId(), buildMessage(calendars)));
    }

    private String buildMessage(List<CalendarListItemDto> calendars) {
        StringBuilder message = new StringBuilder();
        message.append("Список ваших календарей:\n");
        for(int i = 1; i <= calendars.size(); i++)
            message.append(i).append(") ").append(calendars.get(i-1).summary()).append("\n");
        return message.toString();
    }
}
