package com.dagdockersim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(scanBasePackages = "com.dagdockersim")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}


