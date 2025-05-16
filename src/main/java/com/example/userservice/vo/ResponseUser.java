package com.example.userservice.vo;

import com.example.userservice.jpa.Theme;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseUser {
    private String userId;
    private String email;
    private String name;
    private String profileImageUrl;
    private LocalDate birthDate;
    private String theme;
    private String token;
}