package com.example.lab4.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir de 3 à 50 caractères")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Le nom d'utilisateur contient des caractères interdits")
        String username,

        @NotBlank(message = "L'adresse e-mail est obligatoire")
        @Email(message = "L'adresse e-mail n'est pas valide")
        @Size(max = 254, message = "L'adresse e-mail est trop longue")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 12, max = 72, message = "Le mot de passe doit contenir de 12 à 72 caractères")
        String password
) {
}
