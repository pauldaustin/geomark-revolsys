package com.revolsys.ui.web.utils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

public final class HttpRequestUtils {
  private static ThreadLocal<HttpServletRequest> REQUEST_LOCAL = new ThreadLocal<HttpServletRequest>();

  public static void clearHttpServletRequest() {
    REQUEST_LOCAL.remove();
  }

  public static String getFullRequestUrl() {
    return getFullRequestUrl(getHttpServletRequest());
  }

  public static String getFullRequestUrl(final HttpServletRequest request) {
    final String serverUrl = getServerUrl(request);
    final String requestUri = getOriginatingRequestUri();
    return serverUrl + requestUri;
  }

  public static HttpServletRequest getHttpServletRequest() {
    final HttpServletRequest request = REQUEST_LOCAL.get();
    return request;
  }

  public static String getOriginatingRequestUri() {
    final HttpServletRequest request = getHttpServletRequest();
    final String originatingRequestUri = new UrlPathHelper().getOriginatingRequestUri(request);
    return originatingRequestUri;
  }

  public static String getPathVariable(final String name) {
    return getPathVariables().get(name);
  }

  public static Map<String, String> getPathVariables() {
    final HttpServletRequest request = getHttpServletRequest();
    if (request != null) {
      @SuppressWarnings("unchecked")
      Map<String, String> pathVariables = (Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
      if (pathVariables == null) {
        pathVariables = new HashMap<String, String>();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
          pathVariables);
      }
      return pathVariables;
    }
    return new HashMap<String, String>();
  }

  public static <T> T getRequestAttribute(final String name) {
    final HttpServletRequest request = getHttpServletRequest();
    if (request == null) {
      return null;
    } else {
      return (T)request.getAttribute(name);
    }
  }

  public static String getRequestBaseFileName() {
    final String originatingRequestUri = getOriginatingRequestUri();
    final String baseName = WebUtils.extractFilenameFromUrlPath(originatingRequestUri);
    return baseName;
  }

  public static String getRequestFileName() {
    final String originatingRequestUri = getOriginatingRequestUri();
    final String baseName = WebUtils.extractFullFilenameFromUrlPath(originatingRequestUri);
    return baseName;
  }

  public static String getServerUrl() {
    return getServerUrl(getHttpServletRequest());
  }

  public static String getServerUrl(final HttpServletRequest request) {
    final String scheme = request.getScheme();
    final String serverName = request.getServerName();
    final int serverPort = request.getServerPort();
    final StringBuilder url = new StringBuilder();
    url.append(scheme);
    url.append("://");
    url.append(serverName);

    if ("http".equals(scheme)) {
      if (serverPort != 80 && serverPort != -1) {
        url.append(":").append(serverPort);
      }
    } else if ("https".equals(scheme)) {
      if (serverPort != 443 && serverPort != -1) {
        url.append(":").append(serverPort);
      }
    }
    return url.toString();

  }

  public static void setHttpServletRequest(final HttpServletRequest request) {
    REQUEST_LOCAL.set(request);
  }

  public static void setPathVariable(final String name, final String value) {
    getPathVariables().put(name, value);
  }

  private HttpRequestUtils() {

  }
}
