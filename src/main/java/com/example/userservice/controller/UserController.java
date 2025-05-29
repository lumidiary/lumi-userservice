package com.example.userservice.controller;

import com.example.userservice.exception.CodeExpiredException;
import com.example.userservice.exception.CodeMismatchException;
import com.example.userservice.exception.CodeNotFoundException;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(
        origins = {"https://lumi-fe-eta.vercel.app", "http://localhost:5173"},  // 허용할 프론트 도메인
        allowCredentials = "true"                                               // 쿠키·인증헤더 허용
)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 다이제스트 완료 엔드포인트
    @PostMapping("/digest/completed")
    public ResponseEntity<Void> digestCompleted(
            @Valid @RequestBody DigestNotificationRequest req) {
        userService.notifyDigestCompleted(req.getUserId(), req.getDigestContent());
        return ResponseEntity.ok().build();
    }

    // 프로필 이미지 업로드
    @PostMapping(value = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseUser uploadProfileImage(
            @PathVariable String userId,
            @RequestPart("file") MultipartFile file) {
        return userService.updateProfileImage(userId, file);
    }

    // 이메일 인증 코드(JWT) 발송
    @PostMapping("/email/verify")
    public ResponseEntity<String> sendEmailVerify(
            @Valid @RequestBody EmailVerificationRequest req) {
        userService.sendSignupVerification(req.getEmail());
        return ResponseEntity.ok("인증 메일이 발송되었습니다. 메일함을 확인해주세요.");
    }

    // 이메일 인증 코드(JWT) 검증 (GET 방식 - url에 users/email/confirm?token={token})
    @GetMapping("/email/confirm")
    public ResponseEntity<String> confirmEmail(@RequestParam("token") String token) {
        try {
            userService.verifySignupToken(token);
            return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
        } catch (UsernameNotFoundException ex) {
            // DB에 해당 이메일이 없을 때
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    // 이메일 인증 코드(JWT) 검증 (POST 방식 - body에 json)
//    @PostMapping("/email/confirm")
//    public ResponseEntity<String> confirmEmailPost(@RequestBody Map<String,String> body) {
//        String token = body.get("token");
//        try {
//            userService.verifySignupToken(token);
//            return confirmEmail(token);
//        } catch (IllegalArgumentException ex) {
//            return ResponseEntity.badRequest().body(ex.getMessage());
//        }
//    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ResponseUser> signup(@Valid @RequestBody RequestUser req) {
        // 이메일 인증 토큰 검증
        userService.verifySignupToken(req.getToken());

        // signup logic (이메일 중복 등 내부에서 처리)
        ResponseUser resp = userService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
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

    // 비밀번호 재설정 코드 검증 및 비밀번호 변경
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        userService.confirmPasswordReset(req);
        return ResponseEntity.ok().build();
    }

    // 내 프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<ResponseUser> getProfile(Authentication auth) {
        ResponseUser principal = (ResponseUser) auth.getPrincipal();
        String userId = principal.getUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
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

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
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
