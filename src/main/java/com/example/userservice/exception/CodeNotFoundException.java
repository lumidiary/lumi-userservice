package com.example.userservice.exception;

public class CodeNotFoundException extends RuntimeException {
    public CodeNotFoundException(String message) {
        super(message);
    }
}