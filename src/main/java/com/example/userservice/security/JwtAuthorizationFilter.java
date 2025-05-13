package com.example.userservice.security;

import com.example.userservice.service.UserService;
import com.example.userservice.vo.ResponseUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {
    private final Environment env;
    private final UserService userService;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager,
                                  UserService userService,
                                  Environment env) {
        super(authenticationManager);
        this.userService = userService;
        this.env = env;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws IOException, ServletException {

        String token = req.getHeader("token");
        if (token == null) {
            chain.doFilter(req, res);
            return;
        }
        // 2) SecretKey 생성 (application.yml의 jwt.secret 읽어서)
        byte[] keyBytes = Decoders.BASE64.decode(env.getProperty("jwt.secret"));
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        // 3) Jwts.parser() 로 파서 생성 후 토큰 파싱
        Claims claims = Jwts.parser()
                .setSigningKey(key)                // 서명 검증용 키 설정
                .build()
                .parseClaimsJws(token)
                .getBody();                        // 페이로드(Claims) 얻기

        // 4) Subject 에 담긴 userId 추출
        String userId = claims.getSubject();

        // 5) userId 로 사용자 조회 후 Authentication 생성
        if (userId != null) {
            ResponseUser userDetails = userService.getProfile(userId);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            Collections.emptyList()        // 권한이 있다면 여기에 설정
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 다음 필터 체인으로
        chain.doFilter(req, res);
    }
}
