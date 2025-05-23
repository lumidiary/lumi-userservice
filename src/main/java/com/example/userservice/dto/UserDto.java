package com.example.userservice.dto;

import com.example.userservice.jpa.Theme;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    @NotNull
    private String email;
    @NotNull
    private String name;
    @NotNull
    private String pwd;
    @NotNull
    private String userId;
    @NotNull
    private LocalDate birthDate;
    @NotNull
    private Theme theme;
    @NotNull
    private String profileImageUrl;
    @NotNull
    private String encryptedPwd;
    @NotNull
    private String token;

}
