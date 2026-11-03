package com.example.orbit.server.user;

import com.example.orbit.server.auth.AuthInterceptor;
import com.example.orbit.server.user.UserDtos.AvatarUploadRequest;
import com.example.orbit.server.user.UserDtos.UpdateProfileRequest;
import com.example.orbit.server.user.UserDtos.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserDto profile(@RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) long userId) {
        return userService.profile(userId);
    }

    @PatchMapping
    public UserDto updateProfile(
        @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) long userId,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateNickname(userId, request.nickname());
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto uploadAvatar(
        @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) long userId,
        @RequestPart("file") MultipartFile file
    ) {
        return userService.updateAvatar(userId, file);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserDto uploadAvatarJson(
        @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) long userId,
        @Valid @RequestBody AvatarUploadRequest request
    ) {
        return userService.updateAvatar(userId, request.contentBase64(), request.contentType());
    }
}
