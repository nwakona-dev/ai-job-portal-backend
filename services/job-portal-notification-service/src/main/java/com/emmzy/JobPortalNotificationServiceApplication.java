package com.emmzy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JobPortalNotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				JobPortalNotificationServiceApplication.class,
				args
		);
	}
}