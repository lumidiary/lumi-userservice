package com.example.userservice.vo;

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
public class RequestLogin {

    @NotNull
    @Size(min = 2)
    @Email
    private String email;
    @NotNull
    @Size(min = 8)
    private String password;

}
