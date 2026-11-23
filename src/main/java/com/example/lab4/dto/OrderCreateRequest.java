package com.example.lab4.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderCreateRequest(
        @NotEmpty(message = "La commande doit contenir au moins un article")
        List<@Valid OrderItemRequest> items
) {
}
