package com.example.orbit.server.user;

import com.example.orbit.server.common.ApiException;
import com.example.orbit.server.media.AvatarStorage;
import com.example.orbit.server.user.UserDtos.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private final UserRepository users;
    private final AvatarStorage avatarStorage;

    public UserService(UserRepository users, AvatarStorage avatarStorage) {
        this.users = users;
        this.avatarStorage = avatarStorage;
    }

    @Transactional(readOnly = true)
    public UserDto profile(long userId) {
        return UserDto.from(requireUser(userId));
    }

    @Transactional
    public UserDto updateNickname(long userId, String nickname) {
        AppUser user = requireUser(userId);
        String clean = nickname == null ? "" : nickname.trim();
        if (clean.isEmpty() || clean.length() > 24) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "NICKNAME_INVALID",
                "昵称长度需为 1-24 个字符"
            );
        }
        user.setNickname(clean);
        user.setUpdatedAt(System.currentTimeMillis());
        users.save(user);
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateAvatar(long userId, MultipartFile file) {
        AppUser user = requireUser(userId);
        String previous = user.getAvatarPath();
        String stored = avatarStorage.store(file);
        user.setAvatarPath(stored);
        user.setUpdatedAt(System.currentTimeMillis());
        users.save(user);
        avatarStorage.deleteQuietly(previous);
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateAvatar(long userId, String contentBase64, String contentType) {
        AppUser user = requireUser(userId);
        String previous = user.getAvatarPath();
        String stored = avatarStorage.storeBase64(contentBase64, contentType);
        user.setAvatarPath(stored);
        user.setUpdatedAt(System.currentTimeMillis());
        users.save(user);
        avatarStorage.deleteQuietly(previous);
        return UserDto.from(user);
    }

    private AppUser requireUser(long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(
            HttpStatus.UNAUTHORIZED,
            "USER_NOT_FOUND",
            "账号不存在，请重新登录"
        ));
    }
}
