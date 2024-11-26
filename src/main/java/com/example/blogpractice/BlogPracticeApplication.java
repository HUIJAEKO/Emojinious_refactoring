package com.example.blogpractice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BlogPracticeApplication {
	public static void main(String[] args) {
		SpringApplication.run(BlogPracticeApplication.class, args);
	}

}
