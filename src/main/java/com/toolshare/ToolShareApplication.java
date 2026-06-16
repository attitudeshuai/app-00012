package com.toolshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ToolShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(ToolShareApplication.class, args);
    }
}
