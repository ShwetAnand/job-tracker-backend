package com.shwet.job_tracker_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JobTrackerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobTrackerBackendApplication.class, args);
	}
}
