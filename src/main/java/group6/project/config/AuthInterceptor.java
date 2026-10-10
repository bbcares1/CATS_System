// Checks login and account roles before protected pages open.
package group6.project.config;

import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UserService users;

    public AuthInterceptor(UserService users) {
        this.users = users;
    }

    // A Manager is also Staff; an authenticated wrong role gets a clear 403.
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean adminPage =
                path.equals("/admin")
                        || path.startsWith("/admin/")
                        || path.startsWith("/excluded-days");
        User user = users.currentUser(request.getSession(false));
        if (user == null) {
            if (path.endsWith("/receipt") || path.endsWith("/certificate")) {
                response.sendError(401);
            } else {
                response.sendRedirect(
                        request.getContextPath()
                                + (adminPage ? "/admin/login" : "/employee/login"));
            }
            return false;
        }
        boolean allowed;
        if (adminPage) allowed = user instanceof Admin;
        else if (path.equals("/manager") || path.startsWith("/manager/")) {
            allowed = user instanceof Manager;
        } else if (path.equals("/training/calendar")) allowed = true;
        else allowed = user instanceof Staff;
        if (!allowed) response.sendError(403);
        return allowed;
    }
}
