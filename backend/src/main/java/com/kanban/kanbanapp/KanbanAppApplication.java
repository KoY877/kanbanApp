package com.kanban.kanbanapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the Kanban application.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class KanbanAppApplication {

    /**
     * Bootstrap and run the Spring application context.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(KanbanAppApplication.class, args);
    }
}
