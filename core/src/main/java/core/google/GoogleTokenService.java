package core.google;

import core.DTO.RefreshTokenResponse;
import core.DTO.TokenExchangeException;
import core.configs.CoreConfig;
import core.jpa.JPAServise;
import core.jpa.Token;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
public class GoogleTokenService {
    private WebClient webClient;
    private AccessTokenStore tokenStore;
    private CoreConfig cfg;
    private JPAServise jpaServise;

    public GoogleTokenService(@Qualifier("commonWebClient") WebClient webClient,
                              AccessTokenStore tokenStore,
                              CoreConfig cfg,
                              JPAServise jpaServise) {
        this.webClient = webClient;
        this.tokenStore = tokenStore;
        this.cfg = cfg;
        this.jpaServise = jpaServise;
    }

    public String getAccessToken(long userId) {
        return tokenStore.get(Long.toString(userId)).orElseGet(()->requestForToken(userId));
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
                                .flatMap(body -> Mono.error(new TokenExchangeException(
                                        "status=" + resp.statusCode() +
                                                ", body=" + trim(body)
                                )))
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
        Optional<Token> res = jpaServise.findTokenOptional(userId);
        return res.orElseThrow(() -> new IllegalStateException("Could not find token"))
                .getRefreshToken();
    }
}
