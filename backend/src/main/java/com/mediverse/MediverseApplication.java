package com.mediverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan(basePackages = "com.mediverse.model")
@EnableJpaRepositories(basePackages = "com.mediverse.repository")
@ComponentScan(basePackages = "com.mediverse")
public class MediverseApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediverseApplication.class, args);
    }
}
