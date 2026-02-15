package core.google;

import core.configs.CoreConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class GoogleOAuthService {
    @Autowired
    private CoreConfig config;

    @Autowired
    private OAuthStateStore stateStore;

    public String buildAuthUrl(long chatId) throws NoSuchAlgorithmException {
        String state = generateState();
        stateStore.put(state, chatId);
        log.info("Generated OAuth state={} for chatId={}", state, chatId);
        return UriComponentsBuilder.fromUriString(config.authUri())
                .queryParam("redirect_uri",config.redirectUri())
                .queryParam("client_id",config.clientId())
                .queryParam("scope","https://www.googleapis.com/auth/calendar")
                .queryParam("access_type","offline")
                .queryParam("response_type","code")
                .queryParam("state",state)
                .queryParam("prompt","consent")
                .toUriString();
    }

    private String generateState() throws NoSuchAlgorithmException {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SecureRandom.getInstanceStrong().generateSeed(32));
    }
}
