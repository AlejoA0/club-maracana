package com.maracana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MaracanaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaracanaApplication.class, args);
    }
}
