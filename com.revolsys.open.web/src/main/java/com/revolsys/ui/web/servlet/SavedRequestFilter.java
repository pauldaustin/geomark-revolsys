package com.revolsys.ui.web.servlet;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.revolsys.ui.web.utils.HttpRequestUtils;

public class SavedRequestFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final FilterChain filterChain) throws ServletException, IOException {
    final HttpServletRequest savedRequest = HttpRequestUtils.getHttpServletRequest();
    try {
      HttpRequestUtils.setHttpServletRequest(request);
      filterChain.doFilter(request, response);
    } finally {
      if (savedRequest == null) {
        HttpRequestUtils.clearHttpServletRequest();
      } else {
        HttpRequestUtils.setHttpServletRequest(request);
      }
    }
  }
}
