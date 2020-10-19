package com.example.technodomkz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TechnodomkzApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnodomkzApplication.class, args);
    }

}
