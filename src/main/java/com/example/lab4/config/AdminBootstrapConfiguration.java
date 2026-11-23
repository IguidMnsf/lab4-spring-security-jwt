package com.example.lab4.config;

import com.example.lab4.entity.AppUser;
import com.example.lab4.entity.Role;
import com.example.lab4.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrapConfiguration {

    @Bean
    public ApplicationRunner bootstrapAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.core.env.Environment environment
    ) {
        return args -> {
            String username = required(environment, "app.bootstrap-admin.username");
            String email = required(environment, "app.bootstrap-admin.email");
            String password = required(environment, "app.bootstrap-admin.password");
            if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Le mot de passe admin doit contenir de 12 à 72 caractères.");
            }
            if (userRepository.existsByUsername(username)) {
                return;
            }
            if (userRepository.existsByEmail(email)) {
                throw new IllegalStateException("L'adresse e-mail admin est déjà utilisée par un autre compte.");
            }
            userRepository.save(new AppUser(username, email, passwordEncoder.encode(password), Role.ADMIN));
        };
    }

    private String required(org.springframework.core.env.Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("La propriété " + property + " est obligatoire si le bootstrap admin est activé.");
        }
        return value;
    }
}
