package com.example.musicbooru.init;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.example.musicbooru.util.Commons.*;

@Component
public class FirstDeploymentSetup implements ApplicationRunner {

    private static final List<Path> APP_DIRECTORIES = List.of(
            Paths.get(LIBRARY),
            Paths.get(ARTWORK),
            Paths.get(ICON)
    );

    @Override
    public void run(ApplicationArguments args) {
        APP_DIRECTORIES.forEach(dir -> {
           try {
               Files.createDirectories(dir);
           } catch (IOException e) {
               throw new RuntimeException("Could not create directory: " + dir, e);
           }
        });
    }
}
