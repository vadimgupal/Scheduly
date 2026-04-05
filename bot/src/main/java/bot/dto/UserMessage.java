package bot.dto;

public record UserMessage(
        long chatId,
        String username,
        String message,
        String callbackQueryId
) {
    public boolean isCallback() {
        return callbackQueryId != null;
    }

    public static UserMessage text(long chatId, String username, String text) {
        return new UserMessage(chatId, username, text, null);
    }

    public static UserMessage callback(long chatId, String username, String data, String callbackQueryId) {
        return new UserMessage(chatId, username, data, callbackQueryId);
    }
}