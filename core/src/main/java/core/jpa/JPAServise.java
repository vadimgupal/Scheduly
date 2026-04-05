package core.jpa;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
public class JPAServise {
    private TaskRepository taskRepository;
    private TokenRepository tokenRepository;
    private UserRepository userRepository;

    public JPAServise(TaskRepository taskRepository, TokenRepository tokenRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void saveOrUpdateToken(long userId, String refreshToken) {
        Token token = tokenRepository.findById(userId).orElseGet(() -> {
            Token t = new Token();
            t.setUserId(userId);
            return t;
        });

        token.setRefreshToken(refreshToken);
        tokenRepository.save(token);
    }

    @Transactional
    public void saveOrUpdateUser(long chatId, String username) {
        User user = userRepository.findByChatId(chatId).orElseGet(User::new);
        user.setUsername(username);
        user.setChatId(chatId);
        userRepository.save(user);
    }

    public User findUserByChatId(long chatId) {
        Optional<User> u = userRepository.findByChatId(chatId);
        if(u.isEmpty()){
            log.error("User not found");
            throw new RuntimeException("User not found");
        }
        return u.get();
    }

    public Optional<Token> findTokenOptional(long userId) {
        return tokenRepository.findById(userId);
    }

    public void deleteTokenByUserId(long userId) {
        tokenRepository.deleteById(userId);
    }
}
