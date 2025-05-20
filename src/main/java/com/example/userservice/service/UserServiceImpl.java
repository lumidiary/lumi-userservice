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
import org.springframework.web.multipart.MultipartFile;

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
    private final OciStorageService storageService;

    // 로드 사용자
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(username);
        if (userEntity == null) throw new UsernameNotFoundException(username);
        return User.builder()
                .username(userEntity.getEmail())
                .password(userEntity.getEncryptedPwd())
                .authorities(new ArrayList<>())
                .build();
    }

    // 회원가입
    @Override
    public ResponseUser signup(RequestUser req) {

        UserEntity existing = userRepository.findByEmail(req.getEmail());
        if (existing != null && !existing.isDeleted()) {
            // 삭제되지 않은(활성) 계정이 이미 있으면 예외
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        UserEntity userEntity;
        if (existing != null && existing.isDeleted()) {
            // 2-1) 삭제된 계정이 있으면 복원 모드
            userEntity = existing;
            userEntity.restore();                             // 삭제 해제
            userEntity.setEncryptedPwd(
                    passwordEncoder.encode(req.getPwd()));        // 비밀번호 갱신
            userEntity.changeName(req.getName());             // 이름 갱신
            userEntity.changeBirthDate(req.getBirthDate());   // 생년월일 갱신
            userEntity.changeTheme(Theme.LIGHT);        // 테마 갱신
            userEntity.setProfileImageUrl(storageService.getDefaultImageUrl());
        } else {
            // 2-2) 완전 신규 가입
            userEntity = mapper.map(req, UserEntity.class);
            userEntity.setEncryptedPwd(passwordEncoder.encode(req.getPwd()));
            userEntity.setUserId(UUID.randomUUID().toString());
            userEntity.changeName(req.getName());
            userEntity.setProfileImageUrl(storageService.getDefaultImageUrl());
            userEntity.changeBirthDate(req.getBirthDate());
            userEntity.changeTheme(Theme.LIGHT);
        }

        // 3) 저장 (기존 레코드는 UPDATE, 신규는 INSERT)
        userRepository.save(userEntity);
        return mapToResponse(userEntity, null);
    }


    @Override
    public void sendSignupVerification(String email) {
        // DB 조회: 이미 활성화된 계정이 있으면 중복 예외
        UserEntity existing = userRepository.findByEmail(email);
        if (existing != null && !existing.isDeleted()) {
            // 이미 사용 중인(삭제되지 않은) 이메일인 경우 예외 발생
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 삭제된 계정만 있거나 완전 신규 이메일인 경우에만 코드 발송
        emailService.sendVerificationCode(email);
    }

    @Override
    public void verifySignupCode(EmailVerificationRequest req) {
        String email = req.getEmail();
        // DB 조회: 이미 활성 중인 계정이 있으면 중복 예외
        UserEntity existing = userRepository.findByEmail(email);
        if (existing != null && !existing.isDeleted()) {
            // 이미 사용 중인(삭제되지 않은) 이메일이면 예외
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        // 인증 코드 검증: EmailService 에 위임
        if (!emailService.verifyCode(email, req.getCode())) {
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
        // 1) 코드 검증
        if (!emailService.verifyPasswordResetCode(req.getEmail(), req.getCode())) {
            throw new IllegalArgumentException("비밀번호 재설정 코드가 올바르지 않습니다.");
        }

        // 2) 기존 사용자 조회
        UserEntity userEntity = userRepository.findByEmail(req.getEmail());
        if (userEntity == null || userEntity.isDeleted()) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        // 3) 비밀번호만 교체
        userEntity.setEncryptedPwd(passwordEncoder.encode(req.getNewPassword()));

        // 4) 변경된 엔티티 저장 (UPDATE)
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
        UserEntity entity = userRepository.findByUserId(userId);
        if (entity == null || entity.isDeleted()) throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        entity.changeName(req.getName());
        entity.changeBirthDate(req.getBirthDate());
        entity.changeTheme(req.getTheme());
        userRepository.save(entity);
        return mapToResponse(entity, null);
    }

    // 프로필 이미지 수정
    @Override
    public ResponseUser updateProfileImage(String userId, MultipartFile file) {
        UserEntity entity = userRepository.findByUserId(userId);
        if (entity == null || entity.isDeleted()) throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        String url = storageService.uploadProfileImage(userId, file);
        entity.setProfileImageUrl(url);
        userRepository.save(entity);
        return mapToResponse(entity, null);
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
