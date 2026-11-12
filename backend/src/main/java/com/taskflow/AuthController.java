package com.taskflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    record Creds(@NotBlank @Size(min = 3, max = 50) String username, @NotBlank @Size(min = 6, max = 72) String password) {}
    record TokenResponse(String token, String username, String role) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Creds c) {
        if (users.existsByUsername(c.username()))
            return ResponseEntity.status(409).body(Map.of("error", "username taken"));
        User u = new User();
        u.username = c.username();
        u.password = encoder.encode(c.password());
        u.role = "USER";
        users.save(u);
        return ResponseEntity.status(201).body(Map.of("message", "registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Creds c) {
        User u = users.findByUsername(c.username()).orElse(null);
        if (u == null || !encoder.matches(c.password(), u.password))
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        return ResponseEntity.ok(new TokenResponse(jwt.create(u), u.username, u.role));
    }
}
