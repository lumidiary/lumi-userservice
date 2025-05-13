package com.example.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 인증 코드 만료 예외 핸들러
    @ExceptionHandler(CodeExpiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleExpired(CodeExpiredException ex) {
        // "CODE_EXPIRED" 라는 에러코드와 함께 메시지 전달
        return new ApiErrorResponse("CODE_EXPIRED", ex.getMessage());
    }

    // 인증 코드 불일치 예외 핸들러
    @ExceptionHandler(CodeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMismatch(CodeMismatchException ex) {
        return new ApiErrorResponse("CODE_INVALID", ex.getMessage());
    }

    // 인증 코드 자체가 없을 때
    @ExceptionHandler(CodeNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleNotFound(CodeNotFoundException ex) {
        return new ApiErrorResponse("CODE_NOT_FOUND", ex.getMessage());
    }
}