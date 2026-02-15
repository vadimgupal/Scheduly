package core.configs;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record CoreConfig (
    @NotEmpty String authUri,
    @NotEmpty String clientId,
    @NotEmpty String redirectUri,
    @NotEmpty String tokenUri,
    @NotEmpty String clientSecret
){}
