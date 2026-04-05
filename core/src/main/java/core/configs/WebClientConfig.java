package core.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    @Bean(name = "commonWebClient")
    public WebClient getWebClient() {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(5));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
    @Bean(name = "botWebClient")
    public WebClient getBotWebClient() {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(5));
        return WebClient.builder()
                .baseUrl("http://localhost:8080")
                .clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}
