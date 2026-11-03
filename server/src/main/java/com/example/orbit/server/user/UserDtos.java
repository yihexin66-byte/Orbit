package com.example.orbit.server.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(
        Long id,
        String username,
        String nickname,
        String avatarPath,
        long createdAt
    ) {
        public static UserDto from(AppUser user) {
            return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarPath(),
                user.getCreatedAt()
            );
        }
    }

    public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(min = 1, max = 24, message = "昵称长度需为 1-24 个字符")
        String nickname
    ) {
    }

    /**
     * JSON avatar payload used by clients that talk to uniCloud URL化, where
     * multipart bodies are not forwarded to the cloud function.
     */
    public record AvatarUploadRequest(
        @NotBlank(message = "缺少头像数据") String contentBase64,
        @NotBlank(message = "缺少头像类型") String contentType
    ) {
    }
}
