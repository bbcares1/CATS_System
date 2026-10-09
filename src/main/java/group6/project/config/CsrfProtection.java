package group6.project.config;

import jakarta.servlet.http.*;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

@Component("requestDataValueProcessor")
public class CsrfProtection implements HandlerInterceptor, RequestDataValueProcessor {
    public static final String FIELD = "_csrf";
    private static final String SESSION_KEY = "csrfToken";
    private static final String FORM_WRITE = CsrfProtection.class.getName() + ".formWrite";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final SecureRandom RANDOM = new SecureRandom();

    // One unpredictable token belongs to the browser session, including its login form.
    public static String token(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(SESSION_KEY);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(SESSION_KEY, token);
            }
            return token;
        }
    }

    // A successful login starts a new form token along with its new session ID.
    public static void rotate(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    // Every write, including multipart uploads and logout, must carry the session's form token.
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (SAFE_METHODS.contains(request.getMethod())) return true;
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(SESSION_KEY);
        String submitted = request.getParameter(FIELD);
        if (expected != null
                && submitted != null
                && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        submitted.getBytes(StandardCharsets.UTF_8))) return true;
        response.sendError(403, "The form expired. Reload the page and try again.");
        return false;
    }

    // Thymeleaf asks whether each form needs a token while rendering its action.
    @Override
    public String processAction(HttpServletRequest request, String action, String method) {
        request.setAttribute(
                FORM_WRITE, !SAFE_METHODS.contains(method.toUpperCase(java.util.Locale.ROOT)));
        return action;
    }

    // Normal th:action POST forms receive the hidden field without per-page security code.
    @Override
    public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(FORM_WRITE))
                ? Map.of(FIELD, token(request.getSession()))
                : Map.of();
    }

    // Business field values are unchanged.
    @Override
    public String processFormFieldValue(
            HttpServletRequest request, String name, String value, String type) {
        return value;
    }

    // Tokens stay out of URLs, browser history and exported links.
    @Override
    public String processUrl(HttpServletRequest request, String url) {
        return url;
    }
}
