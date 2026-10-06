package de.adamkaddour.task_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:context-test", "spring.jpa.hibernate.ddl-auto=create-drop"})
class TaskTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
