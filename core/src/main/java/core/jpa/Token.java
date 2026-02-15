package core.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tokens")
@Getter
@Setter
public class Token {
    @Id
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "google_refresh_token", nullable = false)
    private String refreshToken;
}
