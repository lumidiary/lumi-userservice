package com.example.userservice.controller;

import com.example.userservice.service.UserService;
import com.example.userservice.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseUser uploadProfileImage(
            @PathVariable String userId,
            @RequestPart("file") MultipartFile file
    ) {
        return userService.updateProfileImage(userId, file);
    }

    // 이메일 중복 확인 및 인증 코드 발송
    @PostMapping("/email/verify")
    public ResponseEntity<String> sendEmailVerify(@Valid @RequestBody EmailVerificationRequest req) {
        try {
            userService.sendSignupVerification(req.getEmail());
            return ResponseEntity.ok("인증 메일이 발송되었습니다. 메일함을 확인해주세요.");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    // 이메일 인증 링크 클릭 (토큰 확인)
    @GetMapping("/email/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        boolean ok = userService.verifySignupToken(token);
        if (!ok) {
            // 잘못된 토큰 경우
            return ResponseEntity
                    .badRequest()
                    .body("유효하지 않은 인증 토큰입니다.");
        }
        // 토큰 검증 성공 안내
        return ResponseEntity
                .ok("이메일 인증이 완료되었습니다.");
    }


    // 이메일 인증 코드 확인
    @PostMapping("/email/confirm")
    public ResponseEntity<String> confirmEmail(@Valid @RequestBody EmailVerificationRequest req) {
        try {
            userService.verifySignupCode(req);
            return ResponseEntity.ok("인증이 완료되었습니다.");
        } catch (IllegalArgumentException ex) {
            // 중복 또는 코드 오류 시 메시지와 함께 400 반환
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
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
        ResponseUser principal = (ResponseUser) auth.getPrincipal();
        // userId 조회 -> fresh data
        String userId = principal.getUserId();
        ResponseUser upToDate = userService.getProfile(userId);
        return ResponseEntity.ok(upToDate);
    }

    // 내 프로필 수정
    @PutMapping("/profile")
    public ResponseEntity<ResponseUser> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest req) {

        ResponseUser principal = (ResponseUser) auth.getPrincipal();
        String userId = principal.getUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // SecurityContext 초기화 (메모리 내의 인증 정보 삭제)
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok().build();
    }

    // 회원 탈퇴 (soft delete)
    @DeleteMapping
    public ResponseEntity<Void> deleteUser(Authentication auth) {
        ResponseUser me = (ResponseUser) auth.getPrincipal();
        userService.deleteUser(me.getUserId());
        return ResponseEntity.noContent().build();
    }

    // 전체 회원 조회 (관리용)
    @GetMapping
    public ResponseEntity<Iterable<ResponseUser>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

}
