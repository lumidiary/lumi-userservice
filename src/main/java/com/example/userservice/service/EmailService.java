package com.example.userservice.service;

public interface EmailService {
    /** 회원가입 이메일 인증 링크 발송 */
    void sendVerificationLink(String email);

    /** 회원가입 이메일 인증 토큰 검증 */
    boolean verifySignupToken(String token);

    /** 비밀번호 재설정 이메일 링크 발송 */
    void sendPasswordResetLink(String email);

    /** 비밀번호 재설정 토큰 검증 */
    boolean verifyPasswordResetToken(String token);
}
