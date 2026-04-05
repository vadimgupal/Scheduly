package core.google;

import core.DTO.RefreshTokenResponse;
import core.DTO.TokenExchangeException;
import core.configs.CoreConfig;
import core.jpa.JPAServise;
import core.jpa.Token;
import core.notification.NotificationBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class GoogleTokenService {
    private WebClient webClient;
    private AccessTokenStore tokenStore;
    private CoreConfig cfg;
    private JPAServise jpa;
    private NotificationBot notificationBot;

    public GoogleTokenService(@Qualifier("commonWebClient") WebClient webClient,
                              AccessTokenStore tokenStore,
                              CoreConfig cfg,
                              JPAServise jpaServise,
                              NotificationBot notificationBot) {
        this.webClient = webClient;
        this.tokenStore = tokenStore;
        this.cfg = cfg;
        this.jpa = jpaServise;
        this.notificationBot = notificationBot;
    }

    public String getAccessTokenByUserId(long userId) {
        return tokenStore.get(Long.toString(userId)).orElseGet(() -> requestForToken(userId));
    }

    public String getAccessTokenByChatId(long chatId) {
        long userId = jpa.findUserByChatId(chatId).getId();
        try {
            return getAccessTokenByUserId(userId);
        } catch (TokenExchangeException e) {
            if (e.isInvalidGrant()) {
                log.warn("Refresh token invalid_grant for userId={} chatId={}. Clearing token and asking reauth.", userId, chatId);

                // 1) удаляем refresh token из БД, чтобы не падать бесконечно
                jpa.deleteTokenByUserId(userId);

                // 2) уведомляем бота
                notificationBot.notifyBot(chatId,
                        "🔒 Доступ к Google истёк или был отозван.\n" +
                                "Нажми /start и пройди авторизацию заново."
                );
            }
            throw e; // пусть контроллер решает какой статус вернуть
        }
    }

    private String requestForToken(long userId) {
        RefreshTokenResponse token = webClient.post()
                .uri(cfg.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", cfg.clientId())
                        .with("client_secret", cfg.clientSecret())
                        .with("grant_type", "refresh_token")
                        .with("refresh_token", getRefreshToken(userId))
                )
                .retrieve()
                .onStatus(st -> st.is4xxClientError() || st.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("<empty body>")
                                .flatMap(body -> {
                                    boolean invalidGrant =
                                            body.contains("invalid_grant") ||
                                            body.contains("Token has been expired");
                                    return Mono.error(
                                            new TokenExchangeException(
                                                    "status=" + resp.statusCode() + ", body=" + trim(body),
                                                    invalidGrant
                                            )
                                    );
                                        }
                                )
                )
                .bodyToMono(RefreshTokenResponse.class)
                .block();

        if(token != null && token.accessToken() != null && !token.accessToken().isBlank()) {
            long ttl = Math.max(1, token.expiresIn() - 10);
            tokenStore.put(userId, token.accessToken(), Duration.ofSeconds(ttl));
            return token.accessToken();
        }

        throw new IllegalStateException("access token not received");
    }

    private String trim(String s) {
        if (s == null) return "";
        s = s.strip();
        if (s.length() <= 800) return s;
        return s.substring(0, 800) + "...";
    }

    private String getRefreshToken(long userId) {
        Optional<Token> res = jpa.findTokenOptional(userId);
        return res.orElseThrow(() -> new IllegalStateException("Could not find token"))
                .getRefreshToken();
    }
}
