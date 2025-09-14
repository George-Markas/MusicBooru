package com.example.musicbooru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@SpringBootApplication
public class MusicBooruApplication {
	public static void main(String[] args) {
		SpringApplication.run(MusicBooruApplication.class, args);
	}
}
