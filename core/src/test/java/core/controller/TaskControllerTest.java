package core.controller;

import core.jpa.JPAServise;
import core.jpa.Task;
import dto.TaskCreateRequest;
import dto.TaskDto;
import dto.TaskUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskControllerTest {

    private JPAServise jpaServise;
    private TaskController controller;

    @BeforeEach
    void setUp() {
        jpaServise = mock(JPAServise.class);
        controller = new TaskController(jpaServise);
    }

    @Test
    void createTaskShouldReturnCreatedTask() {
        OffsetDateTime deadline = OffsetDateTime.parse("2026-05-25T14:00:00+03:00");

        Task task = new Task();
        task.setId(1L);
        task.setDescription("task");
        task.setPriority(3);
        task.setDeadline(deadline);

        when(jpaServise.saveUserTask(123L, "task", 3, deadline))
                .thenReturn(task);

        TaskCreateRequest request = new TaskCreateRequest("task", 3, deadline);

        ResponseEntity<TaskDto> response = controller.createTask(123L, request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("task", response.getBody().description());
        assertEquals(3, response.getBody().priority());
        assertEquals(deadline, response.getBody().deadline());
    }

    @Test
    void createTaskShouldReturn500OnError() {
        OffsetDateTime deadline = OffsetDateTime.parse("2026-05-25T14:00:00+03:00");

        when(jpaServise.saveUserTask(anyLong(), anyString(), anyInt(), any()))
                .thenThrow(new RuntimeException("error"));

        TaskCreateRequest request = new TaskCreateRequest("task", 3, deadline);

        ResponseEntity<TaskDto> response = controller.createTask(123L, request);

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void getTasksShouldReturnTasks() {
        Task task = new Task();
        task.setId(1L);
        task.setDescription("task");
        task.setPriority(3);
        task.setDeadline(OffsetDateTime.parse("2026-05-25T14:00:00+03:00"));

        when(jpaServise.findTasksByChatId(123L))
                .thenReturn(List.of(task));

        ResponseEntity<List<TaskDto>> response = controller.getTasks(123L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("task", response.getBody().getFirst().description());
    }

    @Test
    void getTasksShouldReturn500AndEmptyListOnError() {
        when(jpaServise.findTasksByChatId(123L))
                .thenThrow(new RuntimeException("error"));

        ResponseEntity<List<TaskDto>> response = controller.getTasks(123L);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void updateTaskShouldReturnUpdatedTask() {
        OffsetDateTime deadline = OffsetDateTime.parse("2026-05-25T14:00:00+03:00");

        Task task = new Task();
        task.setId(1L);
        task.setDescription("updated");
        task.setPriority(5);
        task.setDeadline(deadline);

        when(jpaServise.updateUserTask(123L, 1L, "updated", 5, deadline))
                .thenReturn(task);

        TaskUpdateRequest request = new TaskUpdateRequest("updated", 5, deadline);

        ResponseEntity<TaskDto> response = controller.updateTask(123L, 1L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("updated", response.getBody().description());
        assertEquals(5, response.getBody().priority());
    }

    @Test
    void deleteTaskShouldReturnOk() {
        ResponseEntity<String> response = controller.deleteTask(123L, 1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("deleted", response.getBody());

        verify(jpaServise).deleteUserTask(123L, 1L);
    }

    @Test
    void deleteTaskShouldReturn500OnError() {
        doThrow(new RuntimeException("error"))
                .when(jpaServise).deleteUserTask(123L, 1L);

        ResponseEntity<String> response = controller.deleteTask(123L, 1L);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("server_error", response.getBody());
    }
}