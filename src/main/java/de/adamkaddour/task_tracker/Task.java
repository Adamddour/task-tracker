package de.adamkaddour.task_tracker;

public record Task(
        Long id,
        String title,
        String description,
        boolean completed
) {
}