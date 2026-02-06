package bot.Commands;

public interface CommandHandler<T> extends MessageHandler<T> {

    String command();

    default String commandPrefix() {
        return "/" + command();
    }

    @Override
    default boolean shouldBeHandled(String message) {
        return message.equals(commandPrefix()) || message.startsWith(commandPrefix() + " ");
    }

    @Override
    default T validateAndConvert(String message) {
        if (message.equals(commandPrefix() + " ")) {
            return null;
        } else {
            String commandParamsString = message.substring(commandPrefix().length() + 1);
            var commandParams = commandParamsString.split(" ");
            return validateAndConvertCommandParams(commandParams);
        }
    }

    T validateAndConvertCommandParams(String[] commandParams);
}