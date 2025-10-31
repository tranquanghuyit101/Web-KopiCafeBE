package com.kopi.kopi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.CommandLineRunner;

@EnableScheduling // 🟨 Bật scheduler cho CleanupScheduler
@SpringBootTest
@MockBean(CommandLineRunner.class) // prevent DataInit CommandLineRunner from running during tests
class KopiApplicationTests {

	@Test
	void contextLoads() {
	}

}
