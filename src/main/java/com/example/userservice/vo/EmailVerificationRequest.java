package com.example.userservice.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailVerificationRequest {
    // 이메일 인증 요청
    @NotNull
    @Email
    private String email;

    @NotNull
    private String code;

}
