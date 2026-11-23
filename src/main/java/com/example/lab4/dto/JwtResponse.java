package com.example.lab4.dto;

public record JwtResponse(String token, String tokenType, long expiresInSeconds) {
}
