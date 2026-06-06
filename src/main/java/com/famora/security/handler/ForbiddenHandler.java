package com.famora.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * The type Forbidden handler.
 */
@Component
public class ForbiddenHandler implements AccessDeniedHandler, Serializable {
  
  @Serial
  private static final long serialVersionUID = 1L;
  
  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setCharacterEncoding("utf-8");
    response.setHeader("Content-type", "text/plain;charset=utf-8");
    response.getWriter().println("Forbidden Access!");
  }
}
