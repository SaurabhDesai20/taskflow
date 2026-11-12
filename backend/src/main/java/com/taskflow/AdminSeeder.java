package com.taskflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from the ADMIN_PASSWORD env var (never hard-code it in SQL or code). */
@Component
public class AdminSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminPassword;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder, @Value("${app.admin.password}") String adminPassword) {
        this.users = users; this.encoder = encoder; this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminPassword.isBlank() || users.existsByRole("ADMIN")) return;
        User u = new User();
        u.username = "admin";
        u.password = encoder.encode(adminPassword);
        u.role = "ADMIN";
        users.save(u);
    }
}
