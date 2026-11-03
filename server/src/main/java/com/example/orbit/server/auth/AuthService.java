package com.example.orbit.server.auth;

import com.example.orbit.server.auth.AuthDtos.AuthResponse;
import com.example.orbit.server.auth.AuthDtos.LoginRequest;
import com.example.orbit.server.auth.AuthDtos.RegisterRequest;
import com.example.orbit.server.common.ApiException;
import com.example.orbit.server.user.AppUser;
import com.example.orbit.server.user.UserDtos.UserDto;
import com.example.orbit.server.user.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration TOKEN_TTL = Duration.ofDays(30);
    private static final int TOKEN_BYTES = 32;

    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, AuthTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS", "这个用户名已经被注册");
        }
        long now = System.currentTimeMillis();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(username);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.save(user);
        return new AuthResponse(issueToken(user.getId(), now), UserDto.from(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByUsername(request.username().trim())
            .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        long now = System.currentTimeMillis();
        return new AuthResponse(issueToken(user.getId(), now), UserDto.from(user));
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            tokens.deleteById(token);
        }
    }

    private String issueToken(long userId, long now) {
        tokens.deleteByExpiresAtBefore(now);
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        AuthToken token = new AuthToken();
        token.setToken(HexFormat.of().formatHex(bytes));
        token.setUserId(userId);
        token.setCreatedAt(now);
        token.setExpiresAt(now + TOKEN_TTL.toMillis());
        tokens.save(token);
        return token.getToken();
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "用户名或密码不正确");
    }
}
