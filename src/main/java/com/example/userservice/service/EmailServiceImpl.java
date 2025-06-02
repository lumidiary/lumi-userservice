package com.example.userservice.service;

import com.example.userservice.exception.CodeExpiredException;
import com.example.userservice.exception.CodeMismatchException;
import com.example.userservice.exception.CodeNotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.verification-expiration-ms}")
    private long verificationExpirationMs;

    @Value("${app.client.url}")
    private String clientUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 서명용 SecretKey 생성
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // JWT 토큰 생성 (sub에 email, claim "type"에 용도, iat/exp 포함)
    private String createToken(String email, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(email)                               // 이메일을 subject에 저장
                .claim("type", type)                           // 토큰 용도 구분
                .setIssuedAt(Date.from(now))                     // 발급 시각
                .setExpiration(Date.from(now.plusMillis(verificationExpirationMs))) // 만료 시각
                .signWith(getSigningKey())                       // 서명 수행
                .compact();
    }

    // 회원가입용 인증 코드 발송
    @Override
    public void sendVerificationLink(String email) {
        String token = createToken(email, "signup");
        String link = /*clientUrl + */"https://lumi-fe-eta.vercel.app/signup?verifyToken=" + token;

        String subject = "[LumiDiary] 회원가입 이메일 인증";
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif;color:#333;padding:20px;max-width:600px;margin:auto;\">"
                + "  <div style=\"text-align:center;margin-bottom:20px;\">"
                + "    <h1 style=\"margin:0;font-size:24px;color:#7D3C98;\">LumiDiary</h1>"
                + "  </div>"
                + "  <p style=\"font-size:16px;\">안녕하세요!</p>"
                + "  <p style=\"font-size:16px;\">회원가입 인증을 위해 아래 버튼을 클릭해주세요.</p>"
                + "  <div style=\"background:#f5f5f5;padding:15px;text-align:center;margin:20px 0;\">"
                + "    <a href=\"" + link + "\" style=\"display:inline-block;padding:12px 24px;background:#7D3C98;color:#fff;text-decoration:none;border-radius:4px;\">"
                + "      이메일 인증하기"
                + "    </a>"
                + "  </div>"
                + "  <p style=\"font-size:14px;color:#888;\">이 링크는 15분 후 만료됩니다.</p>"
                + "  <p style=\"font-size:14px;\">요청하지 않으셨다면 고객지원으로 문의해주세요.</p>"
                + "  <hr style=\"border:none;border-top:1px solid #eee;margin:30px 0;\"/>"
                + "  <div style=\"font-size:12px;color:#aaa;text-align:center;\">LumiDiary Inc, Seoul, Korea</div>"
                + "</div>";

        sendHtmlMail(email, subject, html);
    }

    @Override
    public boolean verifySignupToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (!"signup".equals(claims.get("type", String.class))) {
                throw new IllegalArgumentException("잘못된 토큰 타입입니다.");
            }
            return true;
        } catch (ExpiredJwtException ex) {
            throw new IllegalArgumentException("인증 링크가 만료되었습니다.");
        } catch (JwtException ex) {
            throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다.");
        }
    }

    // 비밀번호 재설정용 인증 “코드” 발송 (type = "reset")
    @Override
    public void sendPasswordResetLink(String email) {
        String token = createToken(email, "reset");
        String link = /*clientUrl + */"https://lumi-fe-eta.vercel.app/password-change?verifyToken=" + token;

        String subject = "[LumiDiary] 비밀번호 재설정 이메일";
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif;color:#333;padding:20px;max-width:600px;margin:auto;\">"
                + "  <div style=\"text-align:center;margin-bottom:20px;\">"
                + "    <h1 style=\"margin:0;font-size:24px;color:#E74C3C;\">LumiDiary</h1>"
                + "  </div>"
                + "  <p style=\"font-size:16px;\">비밀번호 재설정을 요청하셨습니다.</p>"
                + "  <p style=\"font-size:16px;\">아래 버튼을 클릭하여 비밀번호를 변경해주세요.</p>"
                + "  <div style=\"background:#f5f5f5;padding:15px;text-align:center;margin:20px 0;\">"
                + "    <a href=\"" + link + "\" style=\"display:inline-block;padding:12px 24px;background:#E74C3C;color:#fff;text-decoration:none;border-radius:4px;\">"
                + "      비밀번호 재설정하기"
                + "    </a>"
                + "  </div>"
                + "  <p style=\"font-size:14px;color:#888;\">이 링크는 15분 후 만료됩니다.</p>"
                + "  <p style=\"font-size:14px;\">요청하지 않으셨다면 고객지원으로 문의해주세요.</p>"
                + "  <hr style=\"border:none;border-top:1px solid #eee;margin:30px 0;\"/>"
                + "  <div style=\"font-size:12px;color:#aaa;text-align:center;\">LumiDiary Inc, Seoul, Korea</div>"
                + "</div>";

        sendHtmlMail(email, subject, html);
    }

    // 비밀번호 재설정용 코드 검증
    @Override
    public boolean verifyPasswordResetToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return "reset".equals(claims.get("type", String.class));
        } catch (JwtException ex) {
            return false;
        }
    }

    @Override
    public void sendDigestCompletionEmail(String toEmail,
                                          UUID id,
                                          String title,
                                          LocalDate periodStart,
                                          LocalDate periodEnd,
                                          String summary) {
        String subject = String.format("[LumiDiary] \"%s\" 다이제스트가 완성되었습니다", title);

        // HTML 템플릿 작성
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif;color:#333;padding:20px;max-width:600px;margin:auto;\">"
                + "  <h2 style=\"color:#7D3C98;\">안녕하세요, LumiDiary입니다!</h2>"
                + "  <p>회원님께서 요청하신 다이제스트가 아래 기간으로 완성되었습니다.</p>"
                + "  <ul style=\"font-size:14px;\">"
                + "    <li><strong>제목:</strong> " + title + "</li>"
                + "    <li><strong>기간:</strong> "
                +        periodStart.format(DATE_FMT) + " ~ " + periodEnd.format(DATE_FMT)
                + "    </li>"
                + "  </ul>"
                + "  <div style=\"background:#f5f5f5;padding:15px;margin:20px 0;border-radius:4px;\">"
                + "    <h4 style=\"margin-top:0;\">요약 내용</h4>"
                + "    <pre style=\"white-space:pre-wrap;font-size:14px;\">"
                +       summary
                + "    </pre>"
                + "  </div>"
                + "  <p>감사합니다.</p>"
                + "</div>";

        // 메일 전송
        sendHtmlMail(toEmail, subject, html);
    }

    // 공통 HTML 메일 전송
    private void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(fromEmail, "LumiDiary");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("HTML 메일 전송 실패 to={}: {}", to, ex.getMessage(), ex);
        }
    }

}
