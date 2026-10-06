package de.adamkaddour.task_tracker;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TaskController {
    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/tasks")
    public List<Task> getTasks() {
        return taskRepository.findAll(Sort.by("id")).stream()
                .map(this::toTask)
                .toList();
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@RequestBody CreateTaskRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Der Titel darf nicht leer sein.");
        }
        TaskEntity entity = new TaskEntity(request.title().strip(), request.description());
        return toTask(taskRepository.save(entity));
    }

    @PatchMapping("/tasks/{id}/complete")
    @Transactional
    public Task completeTask(@PathVariable("id") Long id) {
        TaskEntity entity = findTask(id);
        entity.complete();
        // JPA writes this change when the transaction commits.
        return toTask(entity);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteTask(@PathVariable("id") Long id) {
        taskRepository.delete(findTask(id));
    }

    private TaskEntity findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Die Aufgabe wurde nicht gefunden."));
    }

    private Task toTask(TaskEntity entity) {
        return new Task(entity.getId(), entity.getTitle(),
                entity.getDescription(), entity.isCompleted());
    }
}
