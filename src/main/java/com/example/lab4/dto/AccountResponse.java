package com.example.lab4.dto;

import com.example.lab4.entity.Role;

public record AccountResponse(Long id, String username, String email, Role role) {
}
