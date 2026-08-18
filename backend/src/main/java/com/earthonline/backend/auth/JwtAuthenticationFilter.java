package com.earthonline.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        log.debug(
                "JWT filter: {} {}, authorizationHeaderPresent={}",
                request.getMethod(),
                request.getRequestURI(),
                header != null
        );

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                String username =
                        jwtService.extractUsername(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of()
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                log.debug(
                        "JWT authentication established for user={}",
                        username
                );

            } catch (Exception exception) {
                log.warn(
                        "JWT authentication failed for {} {}: {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        exception.getMessage()
                );
                // token 无效时不设置登录身份
            }
        }

        filterChain.doFilter(request, response);
    }
}
