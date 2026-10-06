package de.adamkaddour.task_tracker;

public record CreateTaskRequest(
        String title,
        String description
) {
}