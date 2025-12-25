package com.nutzycraft.pilotai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PilotAiApplication {

	public static void main(String[] args) {
		System.setProperty("spring.classformat.ignore", "true");
		SpringApplication.run(PilotAiApplication.class, args);
	}

}
