package com.example.orbit.server.media;

import com.example.orbit.server.common.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AvatarStorage {

    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final long MAX_BASE64_CHARS = (MAX_BYTES / 3 + 1) * 4;
    private static final String PUBLIC_PREFIX = "/media/avatars/";
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/webp", "webp"
    );

    private final Path root;

    public AvatarStorage(@Value("${orbit.avatar-dir:./data/avatars}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建头像目录 " + root, exception);
        }
    }

    public Path root() {
        return root;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_MISSING", "请选择一张图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE", "头像不能超过 5 MB");
        }
        try (InputStream input = file.getInputStream()) {
            return storeBytes(input.readAllBytes(), file.getContentType());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_SAVE", "头像保存失败");
        }
    }

    public String storeBase64(String contentBase64, String contentType) {
        if (contentBase64 == null || contentBase64.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_MISSING", "请选择一张图片");
        }
        if (contentBase64.length() > MAX_BASE64_CHARS) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE", "头像不能超过 5 MB");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(
                contentBase64.replaceAll("\\s+", "")
            );
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_INVALID", "头像数据不合法");
        }
        return storeBytes(bytes, contentType);
    }

    public String storeBytes(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_MISSING", "请选择一张图片");
        }
        if (bytes.length > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE", "头像不能超过 5 MB");
        }
        String extension = ALLOWED_TYPES.get(normalizeContentType(contentType));
        if (extension == null) {
            throw new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "AVATAR_TYPE",
                "仅支持 JPG、PNG 或 WEBP 图片"
            );
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_PATH", "头像路径不合法");
        }
        try {
            Files.copy(
                new java.io.ByteArrayInputStream(bytes),
                target,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_SAVE", "头像保存失败");
        }
        return PUBLIC_PREFIX + filename;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    public void deleteQuietly(String publicPath) {
        if (publicPath == null || !publicPath.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String filename = publicPath.substring(PUBLIC_PREFIX.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(filename));
        } catch (IOException ignored) {
            // A stale avatar file is harmless; the database already points at the new one.
        }
    }
}
