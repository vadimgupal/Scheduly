package Commands;

public interface MessageHandler<T> {
    String name();

    void handle(long chatId, T someInput);

    default void handle(long chatId, String message) {
        handle(chatId, validateAndConvert(message));
    }

    boolean shouldBeHandled(String message);

    T validateAndConvert(String message);
}
