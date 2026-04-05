package core.controller;

import core.DTO.TokenExchangeException;
import core.configs.CoreConfig;
import core.DTO.TokensResponse;
import core.google.AccessTokenStore;
import core.google.GoogleOAuthService;
import core.google.OAuthStateStore;
import core.jpa.*;
import core.notification.NotificationBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    private WebClient webClient;
    private GoogleOAuthService googleOAuthService;
    private OAuthStateStore stateStore;
    private AccessTokenStore accessTokenStore;
    private CoreConfig config;
    private JPAServise jpa;
    private NotificationBot notificationBot;

    public AuthController(
            @Qualifier("commonWebClient") WebClient webClient,
            GoogleOAuthService googleOAuthService,
            OAuthStateStore stateStore,
            AccessTokenStore accessTokenStore,
            CoreConfig config,
            JPAServise jpa,
            NotificationBot notificationBot
    ) {
        this.webClient = webClient;
        this.googleOAuthService = googleOAuthService;
        this.stateStore = stateStore;
        this.accessTokenStore = accessTokenStore;
        this.config = config;
        this.jpa = jpa;
        this.notificationBot = notificationBot;
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> oauthCallback(@RequestParam(required = false) String code,
                                           @RequestParam String state,
                                           @RequestParam(required = false) String error) {
        if (error != null) {
            return ResponseEntity.badRequest().body("OAuth error: " + error);
        }
        if (state == null || code == null) {
            return ResponseEntity.badRequest().body("Missing state or code");
        }

        log.info("OAuth callback received state={}, codePresent={}", state, code != null);

        Optional<Long> chatIdOpt = stateStore.consume(state);
        log.info("State consume result: {}", chatIdOpt.isPresent() ? "FOUND" : "NOT_FOUND");
        if (chatIdOpt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid or expired state");
        }
        long chatId = chatIdOpt.get();

        User user;
        try {
            user = jpa.findUserByChatId(chatId);
        } catch (Exception e) {
            notificationBot.notifyBot(chatId, "Не нашёл пользователя. Напиши /start ещё раз.");
            return ResponseEntity.status(400).body("User not found");
        }

        TokensResponse tokens;

        try {
            tokens = exchangeCodeForTokens(code);
        } catch (TokenExchangeException e) {
            notificationBot.notifyBot(chatId, "Ошибка получения токена Google: " + e.getMessage());
            return ResponseEntity.status(400).body("Token exchange failed. Go back to Telegram.");
        }

        if(tokens.accessToken() != null && !tokens.accessToken().isBlank()) {
            long ttl = Math.max(0, tokens.expiresIn() - 10);
            accessTokenStore.put(user.getId(), tokens.accessToken(), Duration.ofSeconds(ttl));
        }

        // refresh_token может быть null — это норм
        if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
            jpa.saveOrUpdateToken(user.getId(), tokens.refreshToken());
            notificationBot.notifyBot(chatId, "✅ Авторизация Google завершена. Можно пользоваться календарём.");
            return ResponseEntity.ok("OK, you can close this tab.");
        }

        // refresh не пришёл — пробуем взять из БД
        if (jpa.findTokenOptional(user.getId()).isPresent()) {
            notificationBot.notifyBot(chatId, "✅ Авторизация обновлена (использую ранее сохранённый refresh token).");
            return ResponseEntity.ok("OK, you can close this tab.");
        }

        // и в БД нет — значит оффлайн-доступ не получен
        notificationBot.notifyBot(chatId,
                "⚠️ Google не выдал refresh token, и в базе его нет.\n" +
                        "Нажми /start и авторизуйся ещё раз.\n" +
                        "Если повторяется — удали доступ бота в аккаунте Google и повтори."
        );
        return ResponseEntity.ok("Authorization incomplete. Go back to Telegram and try again.");
    }

    private TokensResponse exchangeCodeForTokens(String code) {
        try {
            return webClient.post()
                    .uri(config.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", config.clientId())
                            .with("client_secret", config.clientSecret())
                            .with("code", code)
                            .with("grant_type", "authorization_code")
                            .with("redirect_uri", config.redirectUri()))
                    .retrieve()
                    .onStatus(st -> st.is4xxClientError() || st.is5xxServerError(),
                            resp -> resp.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(body -> Mono.error(new TokenExchangeException(
                                            "status=" + resp.statusCode() + ", body=" + trim(body)
                                    )))
                    )
                    .bodyToMono(TokensResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new TokenExchangeException("status=" + e.getStatusCode() + ", body=" + trim(body), e);
        } catch (Exception e) {
            throw new TokenExchangeException("network/parse error: " + e.getClass().getSimpleName(), e);
        }
    }

    private String trim(String s) {
        if (s == null) return "";
        s = s.strip();
        if (s.length() <= 800) return s;
        return s.substring(0, 800) + "...";
    }

    @PostMapping("/google/url")
    public ResponseEntity<String> getAuthUri(@RequestParam long chatId, @RequestParam String username) throws NoSuchAlgorithmException {
        String url = googleOAuthService.buildAuthUrl(chatId);
        jpa.saveOrUpdateUser(chatId, username);
        return ResponseEntity.ok(url);
    }
}
