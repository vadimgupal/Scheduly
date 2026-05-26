package core.jpa;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
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

    @Transactional
    public Task saveUserTask(long chatId, String description, int priority, OffsetDateTime deadline) {
        User user = findUserByChatId(chatId);

        Task task = new Task();
        task.setUser(user);
        task.setDescription(description);
        task.setPriority(priority);
        task.setDeadline(deadline);

        return taskRepository.save(task);
    }

    public List<Task> findTasksByChatId(long chatId) {
        User user = findUserByChatId(chatId);

        return taskRepository.findAllByUserOrderByDeadlineAsc(user);
    }

    @Transactional
    public Task updateUserTask(long chatId,
                               long taskId,
                               String description,
                               Integer priority,
                               OffsetDateTime deadline) {
        User user = findUserByChatId(chatId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getUser().getId() != user.getId()) {
                throw new RuntimeException("Access denied");
        }

        if (description != null && !description.isBlank()) {
            task.setDescription(description);
        }

        if (priority != null) {
            task.setPriority(priority);
        }

        if (deadline != null) {
            task.setDeadline(deadline);
        }

        return taskRepository.save(task);
    }

    @Transactional
    public void deleteUserTask(long chatId, long taskId) {
        User user = findUserByChatId(chatId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getUser().getId() != user.getId()) {
            throw new RuntimeException("Access denied");
        }

        taskRepository.delete(task);
    }

    @Transactional
    public void setDefaultCalendar(long chatId, String calendarId) {
        User user = findUserByChatId(chatId);
        user.setDefaultCalendarId(calendarId);
        userRepository.save(user);
    }

    public Optional<String> getDefaultCalendar(long chatId) {
        User user = findUserByChatId(chatId);
        return Optional.ofNullable(user.getDefaultCalendarId());
    }

    @Transactional
    public void deleteDefaultCalendar(long chatId) {
        User user = findUserByChatId(chatId);
        user.setDefaultCalendarId(null);
        userRepository.save(user);
    }

    @Transactional
    public void setUserTimeZone(long chatId, String timeZone) {
        User user = findUserByChatId(chatId);
        user.setTimeZone(timeZone);
        userRepository.save(user);
    }

    public Optional<String> getUserTimeZone(long chatId) {
        User user = findUserByChatId(chatId);
        return Optional.ofNullable(user.getTimeZone());
    }

    @Transactional
    public void deleteUserTimeZone(long chatId) {
        User user = findUserByChatId(chatId);
        user.setTimeZone(null);
        userRepository.save(user);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<Task> findTasksByUser(User user) {
        return taskRepository.findAllByUser(user);
    }
}
