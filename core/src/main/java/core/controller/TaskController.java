package core.controller;

import core.jpa.JPAServise;
import core.jpa.Task;
import dto.TaskCreateRequest;
import dto.TaskDto;
import dto.TaskUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/task")
public class TaskController {
    private final JPAServise jpaServise;

    public TaskController(JPAServise jpaServise) {
        this.jpaServise = jpaServise;
    }

    @PostMapping("/create")
    public ResponseEntity<TaskDto> createTask(
            @RequestParam long chatId,
            @RequestBody TaskCreateRequest request
    ) {
        try {
            Task task = jpaServise.saveUserTask(
                    chatId,
                    request.description(),
                    request.priority(),
                    request.deadline()
            );

            return ResponseEntity.status(201).body(toDto(task));

        } catch (Exception e) {
            log.error("[TASK_CREATE] failed chatId={} request={}", chatId, request, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<TaskDto>> getTasks(
            @RequestParam long chatId
    ) {
        try {
            List<TaskDto> tasks = jpaServise.findTasksByChatId(chatId)
                    .stream()
                    .map(this::toDto)
                    .toList();

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            log.error("[TASK_LIST] failed chatId={}", chatId, e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<TaskDto> updateTask(
            @RequestParam long chatId,
            @RequestParam long taskId,
            @RequestBody TaskUpdateRequest request
    ) {
        try {
            Task task = jpaServise.updateUserTask(
                    chatId,
                    taskId,
                    request.description(),
                    request.priority(),
                    request.deadline()
            );

            return ResponseEntity.ok(toDto(task));

        } catch (Exception e) {
            log.error("[TASK_UPDATE] failed chatId={} taskId={} request={}",
                    chatId, taskId, request, e);

            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteTask(
            @RequestParam long chatId,
            @RequestParam long taskId
    ) {
        try {
            jpaServise.deleteUserTask(chatId, taskId);
            return ResponseEntity.ok("deleted");
        } catch (Exception e) {
            log.error("[TASK_DELETE] failed chatId={} taskId={}", chatId, taskId, e);
            return ResponseEntity.status(500).body("server_error");
        }
    }

    private TaskDto toDto(Task task) {
        return new TaskDto(
                task.getId(),
                task.getDescription(),
                task.getPriority(),
                task.getDeadline()
        );
    }
}
