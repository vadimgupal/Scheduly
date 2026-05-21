package bot.commands.task.inline;

import bot.commands.MessageHandler;
import bot.commands.task.state.TaskStateStore;
import bot.dto.UserMessage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class DeleteTaskInlineHandler implements MessageHandler {

    private static final String PREFIX = "TASK:DELETE:";

    @Autowired
    private TaskStateStore stateStore;
    @Autowired private TelegramBot bot;

    @Qualifier("coreWebClient")
    @Autowired private WebClient webClient;

    @Override
    public String name() {
        return "Delete task";
    }

    @Override
    public boolean shouldBeHandled(UserMessage msg) {
        return msg.isCallback()
                && msg.message() != null
                && msg.message().startsWith(PREFIX);
    }

    @Override
    public void handle(UserMessage msg) {
        try {
            int index = Integer.parseInt(msg.message().substring(PREFIX.length()));

            String taskId = stateStore.getOption(msg.chatId(), index)
                    .orElseThrow(() -> new RuntimeException("Task option not found"));

            webClient.delete()
                    .uri(b -> b.path("/task/delete")
                            .queryParam("chatId", msg.chatId())
                            .queryParam("taskId", taskId)
                            .build())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            if (msg.callbackQueryId() != null) {
                bot.execute(new AnswerCallbackQuery(msg.callbackQueryId()).text("Удалено"));
            }

            bot.execute(new SendMessage(msg.chatId(), "✅ Задача удалена"));

        } catch (Exception e) {
            log.error("[TASK_DELETE] failed chatId={}", msg.chatId(), e);
            bot.execute(new SendMessage(msg.chatId(),
                    "Не удалось удалить задачу"));
        } finally {
            stateStore.clear(msg.chatId());
        }
    }
}