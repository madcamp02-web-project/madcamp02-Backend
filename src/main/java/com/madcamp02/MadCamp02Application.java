package com.madcamp02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MadCamp02Application {

    public static void main(String[] args) {
        SpringApplication.run(MadCamp02Application.class, args);
    }

}
