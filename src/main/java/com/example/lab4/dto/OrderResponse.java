package com.example.lab4.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, LocalDateTime orderDate, List<OrderItemResponse> items, BigDecimal total) {
}
