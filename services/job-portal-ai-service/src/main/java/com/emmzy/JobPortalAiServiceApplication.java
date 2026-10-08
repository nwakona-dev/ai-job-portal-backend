package com.emmzy;

import com.emmzy.config.AIConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AIConfig.class)
public class JobPortalAiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobPortalAiServiceApplication.class, args);
	}
}