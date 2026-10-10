package group6.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authentication;

    // Register one role gate instead of repeating it in each controller.
    public WebConfig(AuthInterceptor authentication) {
        this.authentication = authentication;
    }

    // Include old bookmarked URLs until their replacements are in place.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authentication).addPathPatterns(
                "/admin", "/admin/**", "/manager", "/manager/**", "/staff", "/staff/**",
                "/course-applications", "/course-applications/**",
                "/course-fee-applications", "/course-fee-applications/**",
                "/course-fee-applicaitions/**", "/excluded-days", "/excluded-days/**",
                "/training/calendar")
                .excludePathPatterns("/admin/login");
    }
}
