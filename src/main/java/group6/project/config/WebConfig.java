package group6.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authentication;
    private final CsrfInterceptor csrf;

    // Register one role gate instead of repeating it in each controller.
    public WebConfig(AuthInterceptor authentication, CsrfInterceptor csrf) {
        this.authentication = authentication;
        this.csrf = csrf;
    }

    // Include old bookmarked URLs until their replacements are in place.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authentication)
                .addPathPatterns(
                        "/admin",
                        "/admin/**",
                        "/manager",
                        "/manager/**",
                        "/staff",
                        "/staff/**",
                        "/course-applications",
                        "/course-applications/**",
                        "/course-fee-applications",
                        "/course-fee-applications/**",
                        "/course-fee-applicaitions/**",
                        "/excluded-days",
                        "/excluded-days/**",
                        "/training/calendar")
                .excludePathPatterns("/admin/login");
        registry.addInterceptor(csrf)
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/error", "/css/**", "/js/**", "/favicon.ico");
    }
}
