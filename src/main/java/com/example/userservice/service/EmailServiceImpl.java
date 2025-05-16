package com.example.userservice.service;

import com.example.userservice.exception.CodeExpiredException;
import com.example.userservice.exception.CodeMismatchException;
import com.example.userservice.exception.CodeNotFoundException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from}")
    private String fromEmail;

    private static class CodeInfo {
        private final String code;
        private final Instant createdAt;

        public CodeInfo(String code, Instant createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }
    }

    // 메모리 캐시로 이메일별 코드 만료시간 저장
    private final Map<String, CodeInfo> signupCodes = new ConcurrentHashMap<>();
    private final Map<String, CodeInfo> resetCodes  = new ConcurrentHashMap<>();

    // 회원가입용 인증 코드 발송
    @Override
    public void sendVerificationCode(String email) {
        // 1) 인증 코드 생성 및 저장
        String code = generateCode();
        signupCodes.put(email, new CodeInfo(code, Instant.now()));

        // 2) 메일 제목 설정
        String subject = "[LumiDiary] 회원가입 인증 코드";

        // 3) Udemy 스타일 HTML 본문 구성
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif; color:#333; padding:20px; max-width:600px; margin:auto;\">"
                + "  <div style=\"text-align:center; margin-bottom:20px;\">"
                + "    <h1 style=\"margin:0; font-size:24px; color:#7D3C98;\">LumiDiary</h1>"
                + "  </div>"
                + "  <p style=\"font-size:16px;\">안녕하세요!</p>"
                + "  <p style=\"font-size:16px;\">회원가입 인증을 위해 아래 코드를 입력해주세요.</p>"
                + "  <div style=\"background:#f5f5f5; padding:15px; text-align:center; margin:20px 0;\">"
                + "    <span style=\"font-size:32px; font-weight:bold; letter-spacing:2px;\">" + code + "</span>"
                + "  </div>"
                + "  <p style=\"font-size:14px; color:#888;\">이 코드는 15분 후 만료됩니다.</p>"
                + "  <p style=\"font-size:14px;\">요청하지 않으셨다면 "
                + "    <a href=\"mailto:support@lumidiary.com\" style=\"color:#7D3C98; text-decoration:none;\">고객지원</a>으로 문의해주세요."
                + "  </p>"
                + "  <hr style=\"border:none; border-top:1px solid #eee; margin:30px 0;\"/>"
                + "  <div style=\"font-size:12px; color:#aaa; text-align:center;\">"
                + "    LumiDiary Inc, Seoul, Korea"
                + "  </div>"
                + "</div>";

        // 4) 공통 발송 메서드 호출
        sendHtmlMail(email, subject, html);
    }

    // 회원가입용 코드 검증
    @Override
    public boolean verifyCode(String email, String code) {
        // 1) 저장된 코드 정보 조회
        CodeInfo info = signupCodes.get(email);
        if (info == null) {
            // 코드 자체가 없는 경우
            throw new CodeNotFoundException("인증 코드가 존재하지 않습니다.");
        }

        // 2) 만료 시간(생성 시각 + 15분) 체크
        Instant expiredAt = info.createdAt.plus(Duration.ofMinutes(15));
        if (Instant.now().isAfter(expiredAt)) {
            // 만료된 경우 저장소에서 제거 후 예외 발생
            signupCodes.remove(email);
            throw new CodeExpiredException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        // 3) 코드 일치 여부 확인
        if (!info.code.equals(code)) {
            // 코드가 다를 경우
            throw new CodeMismatchException("인증 코드가 올바르지 않습니다.");
        }

        // 검증 성공 시 저장된 코드 제거 후 true 반환
        signupCodes.remove(email);
        return true;
    }

    // 비밀번호 재설정용 코드 발송
    @Override
    public void sendPasswordResetCode(String email) {
        // 1) 재설정 코드 생성 및 저장
        String code = generateCode();
        resetCodes.put(email, new CodeInfo(code, Instant.now()));

        // 2) 메일 제목 설정
        String subject = "[LumiDiary] 비밀번호 재설정 인증 코드";

        // 3) Udemy 스타일 HTML 본문 구성 (비밀번호 재설정용)
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif; color:#333; padding:20px; max-width:600px; margin:auto;\">"
                + "  <div style=\"text-align:center; margin-bottom:20px;\">"
                + "    <h1 style=\"margin:0; font-size:24px; color:#E74C3C;\">LumiDiary</h1>"
                + "  </div>"
                + "  <p style=\"font-size:16px;\">비밀번호 재설정을 요청하셨습니다.</p>"
                + "  <p style=\"font-size:16px;\">아래 코드를 입력하여 비밀번호를 변경해주세요.</p>"
                + "  <div style=\"background:#f5f5f5; padding:15px; text-align:center; margin:20px 0;\">"
                + "    <span style=\"font-size:32px; font-weight:bold; letter-spacing:2px;\">" + code + "</span>"
                + "  </div>"
                + "  <p style=\"font-size:14px; color:#888;\">이 코드는 15분 후 만료됩니다.</p>"
                + "  <p style=\"font-size:14px;\">요청하지 않으셨다면 "
                + "    <a href=\"mailto:support@lumidiary.com\" style=\"color:#E74C3C; text-decoration:none;\">고객지원</a>으로 문의해주세요."
                + "  </p>"
                + "  <hr style=\"border:none; border-top:1px solid #eee; margin:30px 0;\"/>"
                + "  <div style=\"font-size:12px; color:#aaa; text-align:center;\">"
                + "    LumiDiary Inc, Seoul, Korea"
                + "  </div>"
                + "</div>";

        // 4) 공통 발송 메서드 호출
        sendHtmlMail(email, subject, html);
    }

    // 비밀번호 재설정 코드 검증
    @Override
    public boolean verifyPasswordResetCode(String email, String code) {
        CodeInfo info = resetCodes.get(email);
        if (info == null) {
            return false;
        }

        if (Instant.now().isAfter(info.createdAt.plus(Duration.ofMinutes(15)))) {
            resetCodes.remove(email);
            return false;
        }

        if (info.code.equals(code)) {
            resetCodes.remove(email);
            return true;
        }
        return false;
    }

    @Override
    public boolean verifySignupToken(String token) {
        return false;
    }

    private void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();  // MimeMessage 생성
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setFrom(fromEmail, "LumiDiary");  // 발신자, 발신자명 설정
            helper.setTo(to);                         // 수신자 설정
            helper.setSubject(subject);               // 제목 설정
            helper.setText(htmlContent, true);        // HTML 본문 설정 (두번째 인자 true)

            mailSender.send(message);  // 메일 전송
        } catch (Exception ex) {
            log.error("OCI Email Delivery HTML 메일 전송 실패 to={}: {}", to, ex.getMessage(), ex);
        }
    }

    private String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        return String.valueOf(code);
    }

}
