package com.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final RestTemplate restTemplate; // 외부 API 호출용

    @Value("${email.api.base-url}")
    private String emailApiBaseUrl;

    // 인증 코드 발송
    @Override
    public void sendVerificationCode(String email) {
        restTemplate.postForEntity(
                emailApiBaseUrl + "/send-code",
                Map.of("email", email, "purpose", "signup"),
                Void.class
        );
    }

    // 회원가입용 코드 검증
    @Override
    public boolean verifyCode(String email, String code) {
        ResponseEntity<Boolean> resp = restTemplate.postForEntity(
                emailApiBaseUrl + "/verify-code",
                Map.of("email", email, "code", code, "purpose", "signup"),
                Boolean.class
        );
        return Boolean.TRUE.equals(resp.getBody());
    }

    // 비밀번호 재설정용 코드 발송
    @Override
    public void sendPasswordResetCode(String email) {
        restTemplate.postForEntity(
                emailApiBaseUrl + "/send-code",
                Map.of("email", email, "purpose", "reset"),
                Void.class
        );
    }

    // 비밀번호 재설정 코드 검증
    @Override
    public boolean verifyPasswordResetCode(String email, String code) {
        ResponseEntity<Boolean> resp = restTemplate.postForEntity(
                emailApiBaseUrl + "/verify-code",
                Map.of("email", email, "code", code, "purpose", "reset"),
                Boolean.class
        );
        return Boolean.TRUE.equals(resp.getBody());
    }

    // 토큰 (메일로 보낸 링크 내 토큰) 검증
    @Override
    public boolean verifySignupToken(String token) {
        ResponseEntity<Boolean> resp = restTemplate.getForEntity(
                emailApiBaseUrl + "/verify-email?token=" + token, Boolean.class
        );
        return Boolean.TRUE.equals(resp.getBody());
    }
}
