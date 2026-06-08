package com.jarus.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JarusAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JarusAiApplication.class, args);
    }
}
