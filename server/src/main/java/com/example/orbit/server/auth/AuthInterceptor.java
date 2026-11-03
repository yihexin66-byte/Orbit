package com.example.orbit.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "orbit.userId";
    public static final String TOKEN_ATTRIBUTE = "orbit.token";

    private final AuthTokenRepository tokens;

    public AuthInterceptor(AuthTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(response);
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return reject(response);
        }
        AuthToken stored = tokens.findByToken(token).orElse(null);
        if (stored == null || stored.getExpiresAt() <= System.currentTimeMillis()) {
            return reject(response);
        }
        request.setAttribute(USER_ID_ATTRIBUTE, stored.getUserId());
        request.setAttribute(TOKEN_ATTRIBUTE, token);
        return true;
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"code\":\"UNAUTHORIZED\",\"message\":\"登录状态已失效，请重新登录\"}"
        );
        return false;
    }
}
