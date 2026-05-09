package com.example.musicbooru.config;

import com.example.musicbooru.user.Role;
import com.example.musicbooru.user.User;
import com.example.musicbooru.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.username}")
    private String defaultAdminUsername;

    @Value("${app.default-admin.password}")
    private String defaultAdminPassword;

    @Override
    public void run(String @NonNull ... args) {
        createDefaultAdminUser();
    }

    private void createDefaultAdminUser() {
        if (!userRepository.existsByUsername(defaultAdminUsername)) {
            User defaultAdminUser = User.builder()
                    .username(defaultAdminUsername)
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(defaultAdminUser);
        }
    }
}
