package com.example.userservice.vo;

import com.example.userservice.jpa.Theme;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestUser {

    @NotNull @Email
    private String email;

    @NotNull @Size(min = 8)
    private String pwd;

    @NotNull @Size(min = 2)
    private String name;

    @NotNull
    private LocalDate birthDate;

    @NotNull
    private Theme theme;

    @NotNull
    private String profileImageUrl;

    @NotNull @Size(min = 4, max = 6) // 이메일 인증 시 전달되는 코드
    private String verificationCode;
}
