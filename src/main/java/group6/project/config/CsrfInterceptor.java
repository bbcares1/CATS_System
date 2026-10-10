package group6.project.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class CsrfInterceptor implements HandlerInterceptor {
    // Every MVC form carries a token from its session; external sites cannot read that token.
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("csrfToken");
        if ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) {
            if (token == null) session.setAttribute("csrfToken", UUID.randomUUID().toString());
            return true;
        }
        String submitted = request.getParameter("_csrf");
        if (token == null
                || submitted == null
                || !MessageDigest.isEqual(
                        token.getBytes(StandardCharsets.UTF_8),
                        submitted.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(403, "Reload the page before submitting this form.");
            return false;
        }
        return true;
    }
}
