package com.example.userservice.controller;

import com.example.userservice.service.UserService;
import com.example.userservice.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 이메일 중복 확인 및 인증 코드 발송
    @PostMapping("/email/verify")
    public ResponseEntity<Void> sendEmailVerify(@Valid @RequestBody EmailVerificationRequest req) {
        userService.sendSignupVerification(req.getEmail());
        return ResponseEntity.ok().build();
    }

    // 이메일 인증 링크 클릭 (토큰 확인)
    @GetMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        boolean ok = userService.verifySignupToken(token);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok().build();
    }

    // 이메일 인증 코드 확인
    @PostMapping("/email/confirm")
    public ResponseEntity<Void> confirmEmailVerify(@Valid @RequestBody EmailVerificationRequest req) {
        userService.verifySignupCode(req);
        return ResponseEntity.ok().build();
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ResponseUser> signup(@Valid @RequestBody RequestUser req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signup(req));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ResponseUser> login(@Valid @RequestBody RequestLogin req) {
        ResponseUser resp = userService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(resp);
    }

    // 비밀번호 재설정 이메일 요청
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest req) {
        userService.sendPasswordReset(req);
        return ResponseEntity.ok().build();
    }

    // 비밀번호 재설정 확인
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        userService.confirmPasswordReset(req);
        return ResponseEntity.ok().build();
    }

    // 내 프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<ResponseUser> getProfile(Authentication auth) {
        String userId = ((UserDetails) auth.getPrincipal()).getUsername();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    // 내 프로필 수정
    @PutMapping("/profile")
    public ResponseEntity<ResponseUser> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest req) {
        String userId = ((UserDetails) auth.getPrincipal()).getUsername();
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }

    // 회원 탈퇴 (soft delete)
    @DeleteMapping
    public ResponseEntity<Void> deleteUser(Authentication auth) {
        String userId = ((UserDetails) auth.getPrincipal()).getUsername();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }



    // 전체 회원 조회 (관리용)
    @GetMapping
    public ResponseEntity<Iterable<ResponseUser>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

}
