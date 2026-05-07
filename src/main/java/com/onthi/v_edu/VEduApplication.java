package com.onthi.v_edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VEduApplication {

	public static void main(String[] args) {
		SpringApplication.run(VEduApplication.class, args);
	}

}
