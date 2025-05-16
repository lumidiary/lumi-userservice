package com.example.userservice.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PasswordResetRequest {
    // 비밀번호 재설정 요청 (이메일 발송만)
    @NotNull
    @Email
    private String email;

}
