package bot.commands;

import bot.dto.UserMessage;

public interface MessageHandler {
    String name();

    void handle(UserMessage msg);

    boolean shouldBeHandled(UserMessage msg);
}