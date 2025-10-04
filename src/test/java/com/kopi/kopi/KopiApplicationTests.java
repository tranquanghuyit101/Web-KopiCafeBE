package com.kopi.kopi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 🟨 Bật scheduler cho CleanupScheduler
@SpringBootTest
class KopiApplicationTests {

	@Test
	void contextLoads() {
	}

}
