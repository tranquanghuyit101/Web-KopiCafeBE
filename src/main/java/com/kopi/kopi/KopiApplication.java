package com.kopi.kopi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableScheduling // 🟨 Bật scheduler cho CleanupScheduler
@EnableAsync // 🟨 Bật async để gửi email không chặn response
@SpringBootApplication
public class KopiApplication {

	public static void main(String[] args) {
		SpringApplication.run(KopiApplication.class, args);
	}

}
