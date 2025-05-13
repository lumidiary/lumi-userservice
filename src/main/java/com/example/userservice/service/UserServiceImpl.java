package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.Theme;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;
import com.example.userservice.vo.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ModelMapper mapper;
    private final EmailService emailService;

    // 로드 사용자
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(username);

        if (userEntity == null || userEntity.isDeleted())
            throw new UsernameNotFoundException(username);

        return User.builder()
                .username(userEntity.getEmail())
                .password(userEntity.getEncryptedPwd())
                .authorities(new ArrayList<>())
                .build();
    }

    // 회원가입
    @Override
    public ResponseUser signup(RequestUser req) {
        // 1) 이메일 중복 체크
        if (userRepository.findByEmail(req.getEmail()) != null) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2) 엔티티 매핑 및 암호화
        UserEntity userEntity = mapper.map(req, UserEntity.class);
        userEntity.setEncryptedPwd(passwordEncoder.encode(req.getPwd()));
        userEntity.setUserId(UUID.randomUUID().toString());
        userEntity.setEmail(req.getEmail());
        userEntity.changeName(req.getName());
        userEntity.changeBirthDate(req.getBirthDate());

        userEntity.changeTheme(Theme.LIGHT);
        userEntity.setProfileImageUrl("");

        // 5) 저장
        userRepository.save(userEntity);

        return mapToResponse(userEntity, null);
    }

    @Override
    public void sendSignupVerification(String email) {
        emailService.sendVerificationCode(email);
    }

    @Override
    public void verifySignupCode(EmailVerificationRequest req) {
        if (!emailService.verifyCode(req.getEmail(), req.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }
    }

    // 로그인
    @Override
    public ResponseUser login(String email, String password) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null || !passwordEncoder.matches(password, userEntity.getEncryptedPwd())) {
            throw new BadCredentialsException("로그인 정보가 올바르지 않습니다.");
        }
        return mapToResponse(userEntity, null);
    }

    // 비밀번호 재설정 요청
    @Override
    public void sendPasswordReset(PasswordResetRequest req) {
        UserEntity userEntity = userRepository.findByEmail(req.getEmail());
        if (userEntity == null) throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        emailService.sendPasswordResetCode(req.getEmail());
    }

    @Override
    public void confirmPasswordReset(PasswordResetConfirmRequest req) {
        if (!emailService.verifyPasswordResetCode(req.getEmail(), req.getCode())) {
            throw new IllegalArgumentException("비밀번호 재설정 코드가 올바르지 않습니다.");
        }
        UserEntity userEntity = userRepository.findByEmail(req.getEmail());
        userEntity = UserEntity.builder()
                .encryptedPwd(passwordEncoder.encode(req.getNewPassword()))
                .build();
        userRepository.save(userEntity);
    }

    // 프로필 조회
    @Override
    public ResponseUser getProfile(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null || userEntity.isDeleted()) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }
        return mapToResponse(userEntity, null);
    }

    // 프로필 수정
    @Override
    public ResponseUser updateProfile(String userId, UpdateProfileRequest req) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null || userEntity.isDeleted()) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }
        userEntity.changeName(req.getName());
        userEntity.changeBirthDate(req.getBirthDate());
        userEntity.setProfileImageUrl(req.getProfileImageUrl());
        userEntity.changeTheme(req.getTheme());
        userRepository.save(userEntity);
        return mapToResponse(userEntity, null);
    }

    // 프로필 탈퇴
    @Override
    public void deleteUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity != null && !userEntity.isDeleted()) {
            userEntity.markDeleted(LocalDateTime.now());
            userRepository.save(userEntity);
        }
    }

    @Override
    public Iterable<ResponseUser> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .map(u -> mapToResponse(u, null))
                .toList();
    }

    @Override
    public ResponseUser getUserDetailsByEmail(String email) {
        UserEntity ue = userRepository.findByEmail(email);
        if (ue == null || ue.isDeleted()) {
            throw new UsernameNotFoundException("User not found: " + email);
        }

        return mapToResponse(ue, null);
    }

    @Override
    public boolean verifySignupToken(String token) {
        // EmailServiceImpl 에 구현된 외부 API 호출 로직에 위임
        return emailService.verifySignupToken(token);
    }

    private ResponseUser mapToResponse(UserEntity userEntity, String token) {
        return ResponseUser.builder()
                .userId(userEntity.getUserId())
                .email(userEntity.getEmail())
                .name(userEntity.getName())
                .birthDate(userEntity.getBirthDate())
                .profileImageUrl(userEntity.getProfileImageUrl())
                .theme(userEntity.getTheme().name())
                .token(token)
                .build();
    }
}
