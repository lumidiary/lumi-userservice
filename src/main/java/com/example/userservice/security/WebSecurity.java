package com.example.userservice.security;

import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class WebSecurity {

    private UserService userService;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private Environment env;

    public WebSecurity(Environment env, UserService userService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.env = env;
        this.userService = userService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // AuthenticationManager 설정
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userService).passwordEncoder(bCryptPasswordEncoder);

        AuthenticationManager authenticationManager = authBuilder.build();

        // csrf 비활성화, 세션 STATELESS
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 엔드포인트 권한 설정
                .authorizeHttpRequests(authz -> authz

                        // 인증 관련(`/auth/**`) 은 모두 permitAll
                        .requestMatchers(HttpMethod.POST, "/users/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/users/email/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/email/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/password-reset/request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/password-reset/confirm").permitAll()


                        // 유저 정보 조회/수정/삭제 등은 인증 필요
                        .requestMatchers("/users/**").authenticated()

                        // 기타 공용 리소스
                        .requestMatchers("/health-check", "/h2-console/**").permitAll()

                        // 기타 요청 거부
                        .anyRequest().denyAll()
                )

                // AuthenticationFilter 등록 (/users/login 처리)
                .authenticationManager(authenticationManager)
                .addFilter(getAuthenticationFilter(authenticationManager))

                // IP 로깅 필터
                .addFilterBefore(new IpAddressLoggingFilter(), UsernamePasswordAuthenticationFilter.class)

                // H2 console iframe 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }


    private AuthenticationFilterNew getAuthenticationFilter(AuthenticationManager authenticationManager) throws Exception {

        AuthenticationFilterNew filter = new AuthenticationFilterNew(authenticationManager, userService, env);
        filter.setFilterProcessesUrl("/auth/login");

        return filter;
    }
}
