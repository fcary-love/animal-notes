package com.pettimeline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.pettimeline.mapper")
public class PetTimelineApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetTimelineApplication.class, args);
    }
}
