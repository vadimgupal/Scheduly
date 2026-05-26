package core.google;

import core.configs.CoreConfig;
import core.dto.TokenExchangeException;
import core.jpa.JPAServise;
import core.jpa.Token;
import core.jpa.User;
import core.notification.NotificationBot;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoogleTokenServiceTest {

    private MockWebServer server;

    private AccessTokenStore tokenStore;
    private CoreConfig cfg;
    private JPAServise jpa;
    private NotificationBot notificationBot;
    private GoogleTokenService service;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder().build();

        tokenStore = mock(AccessTokenStore.class);
        cfg = mock(CoreConfig.class);
        jpa = mock(JPAServise.class);
        notificationBot = mock(NotificationBot.class);

        when(cfg.tokenUri()).thenReturn(server.url("/token").toString());
        when(cfg.clientId()).thenReturn("client-id");
        when(cfg.clientSecret()).thenReturn("client-secret");

        service = new GoogleTokenService(
                webClient,
                tokenStore,
                cfg,
                jpa,
                notificationBot
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void getAccessTokenByUserIdShouldReturnCachedToken() {
        when(tokenStore.get("1"))
                .thenReturn(Optional.of("cached-token"));

        String result = service.getAccessTokenByUserId(1L);

        assertEquals("cached-token", result);
        verifyNoInteractions(jpa);
    }

    @Test
    void getAccessTokenByUserIdShouldRefreshTokenWhenCacheMiss() throws Exception {
        Token refreshToken = new Token();
        refreshToken.setUserId(1L);
        refreshToken.setRefreshToken("refresh-token");

        when(tokenStore.get("1"))
                .thenReturn(Optional.empty());
        when(jpa.findTokenOptional(1L))
                .thenReturn(Optional.of(refreshToken));

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "access_token": "new-access-token",
                          "expires_in": 3600
                        }
                        """));

        String result = service.getAccessTokenByUserId(1L);

        assertEquals("new-access-token", result);

        verify(tokenStore).put(
                eq(1L),
                eq("new-access-token"),
                eq(Duration.ofSeconds(3590))
        );

        RecordedRequest request = server.takeRequest();

        assertEquals("POST", request.getMethod());
        assertEquals("/token", request.getPath());

        String body = request.getBody().readUtf8();

        assertTrue(body.contains("client_id=client-id"));
        assertTrue(body.contains("client_secret=client-secret"));
        assertTrue(body.contains("grant_type=refresh_token"));
        assertTrue(body.contains("refresh_token=refresh-token"));
    }

    @Test
    void getAccessTokenByChatIdShouldFindUserAndReturnToken() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);

        when(jpa.findUserByChatId(123L))
                .thenReturn(user);
        when(tokenStore.get("1"))
                .thenReturn(Optional.of("cached-token"));

        String result = service.getAccessTokenByChatId(123L);

        assertEquals("cached-token", result);
    }

    @Test
    void invalidGrantShouldDeleteRefreshTokenAndNotifyBot() {
        User user = new User();
        user.setId(1L);
        user.setChatId(123L);

        Token refreshToken = new Token();
        refreshToken.setUserId(1L);
        refreshToken.setRefreshToken("bad-refresh-token");

        when(jpa.findUserByChatId(123L))
                .thenReturn(user);
        when(tokenStore.get("1"))
                .thenReturn(Optional.empty());
        when(jpa.findTokenOptional(1L))
                .thenReturn(Optional.of(refreshToken));

        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "error": "invalid_grant"
                        }
                        """));

        assertThrows(TokenExchangeException.class,
                () -> service.getAccessTokenByChatId(123L));

        verify(jpa).deleteTokenByUserId(1L);
        verify(notificationBot).notifyBot(eq(123L), contains("авторизацию заново"));
    }
}