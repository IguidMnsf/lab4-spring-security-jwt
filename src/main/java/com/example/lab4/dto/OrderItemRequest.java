package com.example.lab4.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull(message = "L'identifiant du produit est obligatoire")
        @Positive(message = "L'identifiant du produit doit être positif")
        Long productId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        Integer quantity
) {
}
