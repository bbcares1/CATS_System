package group6.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authentication;

    // Keep all protected route prefixes in one place.
    public WebConfig(AuthInterceptor authentication) {
        this.authentication = authentication;
    }

    // Login and static assets remain public; role checks cover both current and legacy workflows.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authentication).addPathPatterns(
                "/admin", "/admin/**", "/manager", "/manager/**", "/staff", "/staff/**",
                "/course-applications", "/course-applications/**",
                "/course-fee-applications", "/course-fee-applications/**",
                "/excluded-days", "/excluded-days/**")
                .excludePathPatterns("/admin/login");
    }
}
