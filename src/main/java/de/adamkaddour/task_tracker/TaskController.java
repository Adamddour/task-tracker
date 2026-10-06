package de.adamkaddour.task_tracker;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TaskController {

    private final List<Task> tasks = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @GetMapping("/tasks")
    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@RequestBody CreateTaskRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Der Titel darf nicht leer sein."
            );
        }

        Task task = new Task(
                nextId.getAndIncrement(),
                request.title().strip(),
                request.description(),
                false
        );

        tasks.add(task);
        return task;
    }
}