package bot.commands;

import bot.dto.UserMessage;

import java.security.NoSuchAlgorithmException;

public interface MessageHandler {
    String name();

    void handle(UserMessage msg);

    boolean shouldBeHandled(UserMessage msg);
}