package com.example.orbit.server.auth;

import com.example.orbit.server.user.UserDtos.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(
            regexp = "[A-Za-z0-9_]{3,24}",
            message = "需为 3-24 位字母、数字或下划线"
        )
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
        String password
    ) {
    }

    public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
