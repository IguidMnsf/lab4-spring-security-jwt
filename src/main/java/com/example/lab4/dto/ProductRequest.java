package com.example.lab4.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Le nom du produit est obligatoire")
        @Size(max = 160, message = "Le nom ne peut pas dépasser 160 caractères")
        String name,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.00", message = "Le prix doit être positif ou nul")
        BigDecimal price,

        @NotNull(message = "L'identifiant de la catégorie est obligatoire")
        @Positive(message = "L'identifiant de catégorie doit être positif")
        Long categoryId
) {
}
