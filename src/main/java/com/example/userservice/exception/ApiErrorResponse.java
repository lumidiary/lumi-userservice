package com.example.userservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {
    private final String code;      // 에러 코드
    private final String message;   // 클라이언트에 보여줄 메시지
}