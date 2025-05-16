package com.example.userservice.exception;

public class CodeMismatchException extends RuntimeException {
    public CodeMismatchException(String message) {
        super(message);
    }
}