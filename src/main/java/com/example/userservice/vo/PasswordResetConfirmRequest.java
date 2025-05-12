package com.example.userservice.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    // 비밀번호 재설정 확인 (코드 + 새 비밀번호)
    @NotNull
    @Email
    private String email;

    @NotNull
    private String code;

    @NotNull
    @Size(min = 8)
    private String newPassword;


}
