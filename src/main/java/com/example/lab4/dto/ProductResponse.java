package com.example.lab4.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, Long categoryId, String categoryName) {
}
