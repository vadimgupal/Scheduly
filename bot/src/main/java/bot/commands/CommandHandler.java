package bot.commands;

import bot.dto.UserMessage;

public interface CommandHandler extends MessageHandler {

    String command();

    default String commandPrefix() {
        return "/" + command();
    }

    @Override
    default boolean shouldBeHandled(UserMessage msg) {
        String message = msg.message();
        return message.equals(commandPrefix()) || message.startsWith(commandPrefix() + " ");
    }

    default String[] validateAndConvert(UserMessage msg) {
        String message = msg.message();
        if(message.equals(commandPrefix())) {
            return new String[0];
        }
        return message.substring(commandPrefix().length()).trim().split("\\s+");
    }
}