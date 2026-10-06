package de.adamkaddour.task_tracker;





import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class TaskPersistenceTests {
    @TempDir
    Path directory;

    @Test
    void endpointsPersistTasksAcrossApplicationRestarts() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("tasks").toString().replace('\\', '/')
                + ";DB_CLOSE_ON_EXIT=FALSE";
        Long firstId;
        Long secondId;
        {
            try (var context = start(url)) {
                var repository = context.getBean(TaskRepository.class);
                assertEquals(400, request(context, "POST", "/tasks",
                        "{\"title\":\"   \"}").statusCode());
                assertEquals(400, request(context, "POST", "/tasks",
                        "{}").statusCode());
                assertEquals(0, repository.count());
                var created = request(context, "POST", "/tasks",
                        "{\"title\":\"  Keep me  \",\"description\":\"Restart test\"}");
                assertEquals(201, created.statusCode());
                var saved = repository.findAll().getFirst();
                firstId = saved.getId();
                assertEquals("Keep me", saved.getTitle());
                assertFalse(saved.isCompleted());
                assertEquals(200, request(context, "PATCH",
                        "/tasks/" + firstId + "/complete", null).statusCode());
                assertEquals(200, request(context, "PATCH",
                        "/tasks/" + firstId + "/complete", null).statusCode());
                assertEquals(404, request(context, "PATCH",
                        "/tasks/999999/complete", null).statusCode());
            }
            try (var context = start(url)) {
                var repository = context.getBean(TaskRepository.class);
                var restored = repository.findById(firstId).orElseThrow();
                assertEquals("Keep me", restored.getTitle());
                assertEquals("Restart test", restored.getDescription());
                assertTrue(restored.isCompleted());
                var listing = request(context, "GET", "/tasks", null);
                assertEquals(200, listing.statusCode());
                assertTrue(listing.body().contains("\"completed\":true"));
                assertTrue(listing.body().contains("Keep me"));
                assertEquals(201, request(context, "POST", "/tasks",
                        "{\"title\":\"Second task\"}").statusCode());
                secondId = repository.findAll().stream()
                        .filter(task -> !task.getId().equals(firstId)).findFirst().orElseThrow().getId();
                assertNotEquals(firstId, secondId);
                var deleted = request(context, "DELETE", "/tasks/" + firstId, null);
                assertEquals(204, deleted.statusCode());
                assertTrue(deleted.body().isEmpty());
                assertTrue(repository.existsById(secondId));
                assertEquals(404, request(context, "DELETE", "/tasks/" + firstId, null).statusCode());
            }
            try (var context = start(url)) {
                var repository = context.getBean(TaskRepository.class);
                assertFalse(repository.existsById(firstId));
                assertTrue(repository.existsById(secondId));
                assertEquals(1, repository.count());
            }
        }
    }

    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(TaskTrackerApplication.class)
                .web(org.springframework.boot.WebApplicationType.NONE)
                .run( "--spring.datasource.url=" + url,
                        "--spring.jpa.hibernate.ddl-auto=update", "--spring.main.banner-mode=off");
    }

    private Response request(ConfigurableApplicationContext context,
            String method, String path, String body) throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(context.getBean(TaskController.class)).build();
        var builder = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .request(org.springframework.http.HttpMethod.valueOf(method), path);
        if (body != null) {
            builder.contentType("application/json").content(body);
        }
        var response = mvc.perform(builder).andReturn().getResponse();
        return new Response(response.getStatus(), response.getContentAsString());
    }

    private record Response(int statusCode, String body) {}
}