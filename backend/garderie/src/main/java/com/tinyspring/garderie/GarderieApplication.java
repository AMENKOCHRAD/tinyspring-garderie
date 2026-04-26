package com.tinyspring.garderie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GarderieApplication {

    public static void main(String[] args) {
        SpringApplication.run(GarderieApplication.class, args);
    }

}
