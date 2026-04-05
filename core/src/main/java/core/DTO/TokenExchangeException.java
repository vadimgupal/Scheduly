package core.DTO;

import lombok.Getter;

@Getter
public class TokenExchangeException extends RuntimeException {
    private boolean invalidGrant;

    public TokenExchangeException(String message, boolean invalidGrant) {
        super(message);
        this.invalidGrant = invalidGrant;
    }

    public TokenExchangeException(String message) {
        super(message);
    }
    public TokenExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
