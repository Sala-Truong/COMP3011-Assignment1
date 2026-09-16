package com.sala.cassette;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CassetteBackendApplication {

    // This is the entry point that starts the Spring Boot application and loads the full context.
    public static void main(String[] args) {
        SpringApplication.run(CassetteBackendApplication.class, args);
    }
}