package com.example.lab4.service;

import com.example.lab4.dto.AccountResponse;
import com.example.lab4.dto.RegisterRequest;
import com.example.lab4.entity.AppUser;
import com.example.lab4.entity.Role;
import com.example.lab4.exception.AuthenticationConflictException;
import com.example.lab4.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Le mot de passe ne doit pas dépasser 72 octets UTF-8.");
        }
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new AuthenticationConflictException("Ce nom d'utilisateur est déjà utilisé.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new AuthenticationConflictException("Cette adresse e-mail est déjà utilisée.");
        }
        AppUser user = userRepository.save(
                new AppUser(username, email, passwordEncoder.encode(request.password()), Role.USER)
        );
        return new AccountResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
