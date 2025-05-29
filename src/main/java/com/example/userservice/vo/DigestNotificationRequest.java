package com.example.userservice.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DigestNotificationRequest {
    @NotNull
    private String userId;

    @NotNull
    private String digestContent;
}