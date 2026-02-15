package core.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("scope") String scope,
        @JsonProperty("token_type") String tokenType
) { }
