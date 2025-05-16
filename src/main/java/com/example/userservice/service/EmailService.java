package com.example.userservice.service;

public interface EmailService {

    void sendVerificationCode(String email);
    boolean verifyCode(String email, String code);
    void sendPasswordResetCode(String email);
    boolean verifyPasswordResetCode(String email, String code);

    boolean verifySignupToken(String token);

}
