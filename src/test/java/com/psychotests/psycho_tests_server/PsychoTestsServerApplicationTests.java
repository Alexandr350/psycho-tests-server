package com.psychotests.psycho_tests_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
		// 1. Изолируем саму БД в памяти уникальным именем
		"spring.datasource.url=jdbc:h2:mem:isolated_test_db;DB_CLOSE_DELAY=-1",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PsychoTestsServerApplicationTests {

	@Test
	void contextLoads() {
	}
}
