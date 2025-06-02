package com.example.userservice.service;

import java.time.LocalDate;
import java.util.UUID;

public interface EmailService {
    // 회원가입 이메일 인증 링크 발송
    void sendVerificationLink(String email);

    // 회원가입 이메일 인증 토큰 검증
    boolean verifySignupToken(String token);

    // 비밀번호 재설정 이메일 링크 발송
    void sendPasswordResetLink(String email);

    // 비밀번호 재설정 토큰 검증
    boolean verifyPasswordResetToken(String token);

    // 다이제스트 완성 알림 이메일
    public void sendDigestCompletionEmail(String toEmail, UUID id, String title,
                                          LocalDate periodStart, LocalDate periodEnd, String summary);
}
