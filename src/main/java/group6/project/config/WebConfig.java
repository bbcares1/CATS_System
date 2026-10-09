package group6.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authentication;
    private final CsrfProtection csrf;

    // Keep all protected route prefixes in one place.
    public WebConfig(AuthInterceptor authentication, CsrfProtection csrf) {
        this.authentication = authentication;
        this.csrf = csrf;
    }

    // The classroom UI is English on every browser.
    @org.springframework.context.annotation.Bean
    public org.springframework.web.servlet.LocaleResolver localeResolver() {
        return new org.springframework.web.servlet.i18n.FixedLocaleResolver(java.util.Locale.ENGLISH);
    }

    // Login and static assets remain public; role checks cover both current and legacy workflows.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrf).addPathPatterns("/**").excludePathPatterns("/css/**", "/js/**", "/error", "/favicon.ico");
        registry.addInterceptor(authentication).addPathPatterns(
                "/training/calendar", "/admin", "/admin/**", "/manager", "/manager/**", "/staff", "/staff/**",
                "/course-applications", "/course-applications/**",
                "/course-fee-applications", "/course-fee-applications/**",
                "/excluded-days", "/excluded-days/**")
                .excludePathPatterns("/admin/login");
    }
}
