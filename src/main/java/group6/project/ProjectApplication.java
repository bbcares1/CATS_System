package group6.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectApplication {

    // Start the MVC application with its selected database profile.
    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }
}
