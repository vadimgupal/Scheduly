package core.google;

import core.configs.CoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoogleOAuthServiceTest {

    private CoreConfig config;
    private OAuthStateStore stateStore;
    private GoogleOAuthService service;

    @BeforeEach
    void setUp() {
        config = mock(CoreConfig.class);
        stateStore = mock(OAuthStateStore.class);

        service = new GoogleOAuthService();

        ReflectionTestUtils.setField(service, "config", config);
        ReflectionTestUtils.setField(service, "stateStore", stateStore);

        when(config.authUri()).thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
        when(config.redirectUri()).thenReturn("http://localhost:8081/auth/google/callback");
        when(config.clientId()).thenReturn("client-id");
    }

    @Test
    void buildAuthUrlShouldSaveStateAndReturnGoogleUrl() throws Exception {
        String url = service.buildAuthUrl(123L);

        verify(stateStore).put(anyString(), eq(123L));

        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth"));
        assertTrue(url.contains("redirect_uri="));
        assertTrue(url.contains("client_id=client-id"));
        assertTrue(url.contains("scope="));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("state="));
        assertTrue(url.contains("prompt=consent"));
    }
}