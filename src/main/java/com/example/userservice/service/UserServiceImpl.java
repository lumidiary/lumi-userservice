package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.Theme;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;
import com.example.userservice.vo.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.LocalDate;
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

    @Value("${jwt.secret}")
    private String jwtSecret;

    // JWT 서명 키 생성
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

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
        // 1) JWT 토큰 파싱 및 유효성 검증
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(req.getToken())
                    .getBody();
        } catch (ExpiredJwtException ex) {
            throw new IllegalArgumentException("인증 토큰이 만료되었습니다.");
        } catch (JwtException ex) {
            throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다.");
        }
        // 토큰 타입 및 이메일 일치 여부 확인
        if (!"signup".equals(claims.get("type", String.class)) ||
                !claims.getSubject().equals(req.getEmail())) {
            throw new IllegalArgumentException("토큰 검증에 실패했습니다.");
        }

        // 2) 기존 사용자 조회
        UserEntity userEntity = userRepository.findByEmail(req.getEmail());
        if (userEntity == null) {
            userEntity = UserEntity.builder()
                    .userId(UUID.randomUUID().toString())            // UUID 생성
                    .email(req.getEmail())                           // 이메일 필수 세팅
                    .build();
        } else if (userEntity.isDeleted()) {
            // 복구 가입
            userEntity.restore();
        } else {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        
        userEntity.setEncryptedPwd(passwordEncoder.encode(req.getPwd()));
        userEntity.changeName(req.getName());
        userEntity.changeBirthDate(req.getBirthDate());
        userEntity.changeTheme(Theme.LIGHT);
        userEntity.setProfileImageUrl(storageService.getDefaultImageUrl());
        // 이메일 인증된 상태로 설정
        userEntity.verifyEmail();

        // 4) DB 저장 및 응답
        userRepository.save(userEntity);
        return mapToResponse(userEntity, null);
    }

    @Override
    public void notifyDigestCompleted(UUID userId,
                                      String title,
                                      LocalDate periodStart,
                                      LocalDate periodEnd,
                                      String summary) {
        // 1) UUID로 사용자 조회
        UserEntity user = userRepository.findByUserId(userId.toString());
        // 없는 사용자면 404 에러 던지기
        if (user == null || user.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId);
        }

        // 2) 이메일로 다이제스트 완료 알림 전송
        emailService.sendDigestCompletionEmail(user.getEmail(), userId, title, periodStart, periodEnd, summary);
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
        emailService.sendVerificationLink(email);
    }

    @Override
    public boolean verifySignupToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (!"signup".equals(claims.get("type",String.class))) {
            throw new IllegalArgumentException("잘못된 토큰 타입");
        }
        return true;
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
        emailService.sendPasswordResetLink(req.getEmail());
    }

    // 비밀번호 재설정 토큰 검증 및 비밀번호 변경
    @Override
    public void confirmPasswordReset(PasswordResetConfirmRequest req) {
        try {
            boolean ok = emailService.verifyPasswordResetToken(req.getToken());
            if (!ok) {
                throw new IllegalArgumentException("유효하지 않은 비밀번호 재설정 토큰입니다.");
            }
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(req.getToken())
                    .getBody();
            String email = claims.getSubject();
            UserEntity userEntity = userRepository.findByEmail(email);
            if (userEntity == null || userEntity.isDeleted()) {
                throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
            }
            userEntity.setEncryptedPwd(passwordEncoder.encode(req.getNewPassword()));
            userRepository.save(userEntity);
        } catch (ExpiredJwtException ex) {
            throw new IllegalArgumentException("비밀번호 재설정 토큰이 만료되었습니다.");
        } catch (JwtException ex) {
            throw new IllegalArgumentException("유효하지 않은 비밀번호 재설정 토큰입니다.");
        }
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
