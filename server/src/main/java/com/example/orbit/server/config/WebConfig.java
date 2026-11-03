package com.example.orbit.server.config;

import com.example.orbit.server.auth.AuthInterceptor;
import com.example.orbit.server.media.AvatarStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AvatarStorage avatarStorage;

    public WebConfig(AuthInterceptor authInterceptor, AvatarStorage avatarStorage) {
        this.authInterceptor = authInterceptor;
        this.avatarStorage = avatarStorage;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/register",
                "/api/auth/login",
                "/api/health"
            );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/avatars/**")
            .addResourceLocations(avatarStorage.root().toUri().toString());
    }
}
