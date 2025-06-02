package com.example.userservice.security;

import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurity {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Environment env;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 1) AuthenticationManagerBuilder 가져와서 UserDetailsService+PasswordEncoder 설정
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder.userDetailsService(userService).passwordEncoder(passwordEncoder);
        AuthenticationManager authenticationManager = authBuilder.build();

        // 2) DaoAuthenticationProvider 직접 만들어서 http에 등록
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(userService);    // 유저 조회용 서비스 등록
        daoProvider.setPasswordEncoder(passwordEncoder);   // 비밀번호 암호화 방식 등록

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화, 세션도 안 씁니다
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // AuthenticationManager와 Provider 등록
                .authenticationManager(authenticationManager)
                .authenticationProvider(daoProvider)

                // 엔드포인트별 권한 설정
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 에러 핸들러 접근 허용
                        .requestMatchers("/error").permitAll()

                        // 인증 없이 허용할 API
                        .requestMatchers(HttpMethod.POST, "/users/digest/completed").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/email/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/email/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/email/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/password-reset/request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/password-reset/confirm").permitAll()
                        .requestMatchers("/health-check", "/h2-console/**").permitAll()
                        .requestMatchers("/users/logout").permitAll()

                        // 그 외는 인증 필요 또는 거부
                        .requestMatchers("/users/**").authenticated()
                        .anyRequest().denyAll()
                )

                // 로그인 필터와 IP 로깅 필터
                .addFilter(getAuthenticationFilter(authenticationManager))
                .addFilterBefore(new IpAddressLoggingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthorizationFilter(authenticationManager, userService, env),
                        UsernamePasswordAuthenticationFilter.class)

                // H2 콘솔 iframe 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    private AuthenticationFilterNew getAuthenticationFilter(AuthenticationManager authenticationManager) {
        AuthenticationFilterNew filter = new AuthenticationFilterNew(authenticationManager, userService, env);
        filter.setFilterProcessesUrl("/users/login");
        return filter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 프론트 개발 환경 URL
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "https://lumi-fe-eta.vercel.app"
        ));
        // 허용할 HTTP 메서드
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        // 허용할 헤더
        config.setAllowedHeaders(Arrays.asList("*"));
        // 쿠키 인증 등을 위해 필요
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 엔드포인트에 적용
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
