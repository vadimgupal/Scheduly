package core.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class NotificationBot {
    @Autowired
    @Qualifier("botWebClient")
    private WebClient botWebClient;
    public void notifyBot(long chatId, String text) {
        try {
            botWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/notify")
                            .queryParam("chatId", chatId)
                            .build())
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(text)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to notify bot: {}", e.getMessage(), e);
        }
    }
}
