package group6.project.config;

import group6.project.model.*;
import group6.project.service.UserService;

import jakarta.servlet.http.*;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UserService users;

    // Use the shared session resolver, including its stale-account check.
    public AuthInterceptor(UserService users) {
        this.users = users;
    }

    // Role gates also cover retired routes so a bookmarked URL cannot bypass them.
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        User user = users.currentUser(request.getSession(false));
        boolean admin =
                path.equals("/admin")
                        || path.startsWith("/admin/")
                        || path.startsWith("/excluded-days");
        boolean manager = path.equals("/manager") || path.startsWith("/manager/");
        boolean allowed =
                path.equals("/training/calendar")
                        ? user != null
                        : admin
                                ? user instanceof Admin
                                : manager ? user instanceof Manager : user instanceof Staff;
        if (allowed) return true;
        if (path.endsWith("/receipt") || path.endsWith("/certificate")) {
            response.sendError(user == null ? 401 : 403);
        } else {
            response.sendRedirect(
                    request.getContextPath()
                            + (path.equals("/training/calendar")
                                    ? "/login"
                                    : admin ? "/admin/login" : "/employee/login"));
        }
        return false;
    }
}
