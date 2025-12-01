package com.graduate.novel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class NovelApplication {

    public static void main(String[] args) {
        // Set default timezone to UTC to avoid timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(NovelApplication.class, args);
    }

}
