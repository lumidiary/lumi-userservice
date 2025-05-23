package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.vo.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends UserDetailsService {

    ResponseUser signup(RequestUser request);                        // 회원가입
    void sendSignupVerification(String email);                      // 가입 이메일 인증 요청
    boolean verifySignupToken(String token);

    ResponseUser login(String email, String password);              // 로그인 로직

    void sendPasswordReset(PasswordResetRequest req);               // 비밀번호 재설정 이메일 발송
    void confirmPasswordReset(PasswordResetConfirmRequest req);     // 비밀번호 재설정 확인

    ResponseUser getProfile(String userId);                        // 내 프로필 조회
    ResponseUser updateProfile(String userId, UpdateProfileRequest req);           // 내 프로필 수정
    ResponseUser updateProfileImage(String userId, MultipartFile file);            // 프로필 이미지 업로드 및 변경

    void deleteUser(String userId);                                // 탈퇴 (삭제 표시)

    ResponseUser getUserDetailsByEmail(String email);              // 이메일로 회원 정보 조회

    Iterable<ResponseUser> getAllUsers();                          // 전체 회원 조회
}