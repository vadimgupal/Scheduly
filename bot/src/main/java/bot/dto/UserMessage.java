package bot.dto;

public record UserMessage(
        long chatId,
        String username,
        String message
) {}