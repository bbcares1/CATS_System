package group6.project.controller.SecurityForAuthentication;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component 
public class AuthInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) throws Exception {

    String path = request.getRequestURI().substring(request.getContextPath().length());

      if (path.equals("/admin/login")) {
        return true;
      }

    HttpSession session = request.getSession(false);
    
    Object user = session == null ? null : session.getAttribute("user");

      if (user == null) {
        String loginUrl = path.equals("/admin") || path.startsWith("/admin/") ? "/admin/login" : "/employee/login";
        response.sendRedirect(request.getContextPath() + loginUrl);
        return false;
      }

      boolean allowed = false;      
      if (path.equals("/admin") || path.startsWith("/admin/")) {
        allowed = user instanceof Admin;
      } else if (path.equals("/manager") || path.startsWith("/manager/")) {
        allowed = user instanceof Manager;
      } else if (path.equals("/staff") || path.startsWith("/staff/")) {
        allowed = user instanceof Staff && !(user instanceof Manager);
      }

      if (!allowed) {
        response.sendError(HttpStatus.FORBIDDEN.value(),
        "You do not have permission to access this page.");
        return false;
      }

        return true;
    }
}
