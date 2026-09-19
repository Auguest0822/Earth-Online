package com.earthonline.backend.config;

import com.earthonline.backend.auth.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // SSE 流式接口（/api/ai/yearly-memory/stream）在结束时会被容器再派发一次（ASYNC dispatch）。
                        // Spring Security 默认对这一趟也做鉴权，而这一趟请求里没有 JWT 过滤器重新认证，
                        // 于是会被判成“未登录”，异常又因为响应已经提交而无法处理，最终导致流被硬生生掐断。
                        // 放行 ASYNC 是安全的：它不是新的外部请求，只是同一个请求的继续，
                        // 真正的入口请求（DispatcherType.REQUEST）仍然完整地走过 JWT 鉴权。
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(this::isPublicRequest).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"登录状态已过期，请重新登录\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"没有权限执行此操作\"}");
                        }))
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        return "OPTIONS".equals(method)
                || ("GET".equals(method) && "/api/health".equals(uri))
                || ("GET".equals(method) && "/ws/chat".equals(uri))
                || ("POST".equals(method) && "/api/auth/login".equals(uri))
                || ("POST".equals(method) && "/api/auth/register".equals(uri));
    }
}
