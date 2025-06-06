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
    private String profileImageUrl;

    @NotNull
    private String token;
}
