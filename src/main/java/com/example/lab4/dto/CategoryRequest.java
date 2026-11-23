package com.example.lab4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Le nom de la catégorie est obligatoire")
        @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères")
        String name
) {
}
