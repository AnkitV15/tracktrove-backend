package com.tracktrove;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class TracktroveBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(TracktroveBackendApplication.class, args);
	}
}
