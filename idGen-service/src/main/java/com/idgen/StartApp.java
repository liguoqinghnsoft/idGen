package com.idgen;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.ActiveProfiles;

@SpringBootApplication
@ActiveProfiles("test")
@EnableAutoConfiguration
public class StartApp {

    public static void main(String args[]) {
        SpringApplication.run(StartApp.class, args);
    }
}
