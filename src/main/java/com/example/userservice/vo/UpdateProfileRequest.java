package com.example.userservice.vo;

import com.example.userservice.jpa.Theme;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @NotNull
    @Size(min = 2)
    private String name;

    @NotNull
    private LocalDate birthDate;

    @NotNull
    private String profileImageUrl;

    @NotNull
    private Theme theme;

}
