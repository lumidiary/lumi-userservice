package com.example.userservice.dto;

import com.example.userservice.jpa.Theme;
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

    private String email;
    private String name;
    private String pwd;
    private String userId;
    private LocalDate birthDate;
    private Theme theme;
    private String profileImageUrl;
    private String encryptedPwd;


}
