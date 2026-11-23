package com.example.lab4.exception;

public class AuthenticationConflictException extends RuntimeException {
    public AuthenticationConflictException(String message) {
        super(message);
    }
}
