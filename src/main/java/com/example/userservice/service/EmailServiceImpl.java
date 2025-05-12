package com.example.userservice.service;

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

    // 메모리 캐시로 인증 코드 저장 (테스트 후 실제 서비스에서는 DB로 수정할 예정)
    private final Map<String, String> signupCodes = new ConcurrentHashMap<>();
    private final Map<String, String> resetCodes  = new ConcurrentHashMap<>();

    // 회원가입용 인증 코드 발송
    @Override
    public void sendVerificationCode(String email) {
        String code = generateCode();
        signupCodes.put(email, code);

        String subject = "[LumiDiary] 회원가입 인증 코드";
        // HTML 이메일 본문 구성
        String html = "<div style='margin:20px;'>" +
                "<h2>안녕하세요, LumiDiary입니다!</h2>" +
                "<p>회원가입 인증을 위해 아래 코드를 입력해주세요.</p>" +
                "<div style='padding:10px; border:1px solid #ccc; display:inline-block;'>" +
                "<strong style='font-size:18px; color:#2F8FED;'>" + code + "</strong>" +
                "</div>" +
                "<p>감사합니다.</p>" +
                "</div>";

        sendHtmlMail(email, subject, html);
    }

    // 회원가입용 코드 검증
    @Override
    public boolean verifyCode(String email, String code) {
        String saved = signupCodes.get(email);
        if (saved != null && saved.equals(code)) {
            signupCodes.remove(email);
            return true;
        }
        return false;
    }

    // 비밀번호 재설정용 코드 발송
    @Override
    public void sendPasswordResetCode(String email) {
        String code = generateCode();
        resetCodes.put(email, code);

        String subject = "[LumiDiary] 비밀번호 재설정 인증 코드";
        String html = "<div style='margin:20px;'>" +
                "<h2>비밀번호 재설정 요청</h2>" +
                "<p>아래 코드를 입력하여 비밀번호를 재설정하세요.</p>" +
                "<div style='padding:10px; border:1px solid #ccc; display:inline-block;'>" +
                "<strong style='font-size:18px; color:#E74C3C;'>" + code + "</strong>" +
                "</div>" +
                "<p>문의사항이 있으면 support@lumidiary.com으로 연락주세요.</p>" +
                "</div>";

        sendHtmlMail(email, subject, html);
    }

    // 비밀번호 재설정 코드 검증
    @Override
    public boolean verifyPasswordResetCode(String email, String code) {
        String saved = resetCodes.get(email);
        if (saved != null && saved.equals(code)) {
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
