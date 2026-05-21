package bot.commands.task.state;

import bot.redis.RedisStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TaskStateStore {

    @Autowired
    private RedisStore redis;

    private static final Duration DURATION = Duration.ofMinutes(10);

    private static final String STATE_PREFIX = "task:state:";
    private static final String MODE_PREFIX = "task:mode:";
    private static final String DRAFT_PREFIX = "task:draft:";
    private static final String TARGET_PREFIX = "task:target:";
    private static final String OPTION_PREFIX = "task:options:";
    private static final String SKIP_PREFIX = "task:skip:";

    public void putState(long chatId, TaskState state) {
        redis.put(STATE_PREFIX, String.valueOf(chatId), state.name(), DURATION);
    }

    public Optional<TaskState> getState(long chatId) {
        return redis.get(STATE_PREFIX, String.valueOf(chatId))
                .map(TaskState::valueOf);
    }

    public void putMode(long chatId, TaskFlowMode mode) {
        redis.put(MODE_PREFIX, String.valueOf(chatId), mode.name(), DURATION);
    }

    public Optional<TaskFlowMode> getMode(long chatId) {
        return redis.get(MODE_PREFIX, String.valueOf(chatId))
                .map(TaskFlowMode::valueOf);
    }

    public void putDraft(long chatId, String draft) {
        redis.put(DRAFT_PREFIX, String.valueOf(chatId), draft, DURATION);
    }

    public Optional<String> getDraft(long chatId) {
        return redis.get(DRAFT_PREFIX, String.valueOf(chatId));
    }

    public void putTargetTask(long chatId, String taskId) {
        redis.put(TARGET_PREFIX, String.valueOf(chatId), taskId, DURATION);
    }

    public Optional<String> getTargetTask(long chatId) {
        return redis.get(TARGET_PREFIX, String.valueOf(chatId));
    }

    public void putOption(long chatId, int index, String value) {
        redis.put(OPTION_PREFIX + chatId + ":", String.valueOf(index), value, DURATION);
    }

    public Optional<String> getOption(long chatId, int index) {
        return redis.get(OPTION_PREFIX + chatId + ":", String.valueOf(index));
    }

    public int incrementSkipCount(long chatId) {
        int current = redis.get(SKIP_PREFIX, String.valueOf(chatId))
                .map(Integer::parseInt)
                .orElse(0);

        int next = current + 1;
        redis.put(SKIP_PREFIX, String.valueOf(chatId), String.valueOf(next), DURATION);
        return next;
    }

    public void clear(long chatId) {
        redis.consume(STATE_PREFIX, String.valueOf(chatId));
        redis.consume(MODE_PREFIX, String.valueOf(chatId));
        redis.consume(DRAFT_PREFIX, String.valueOf(chatId));
        redis.consume(TARGET_PREFIX, String.valueOf(chatId));
        redis.consume(SKIP_PREFIX, String.valueOf(chatId));
    }
}