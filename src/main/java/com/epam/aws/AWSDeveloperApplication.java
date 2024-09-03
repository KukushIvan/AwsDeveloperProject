package com.epam.aws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AWSDeveloperApplication {
    public static void main(String[] args) {
        SpringApplication.run(AWSDeveloperApplication.class, args);
    }
}
