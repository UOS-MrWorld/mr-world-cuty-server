package com.mrworld.yaho.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${auth.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${auth.rate-limit.requests-per-minute:20}")
    private int limit;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/v1/auth/") || !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long minute = Instant.now().getEpochSecond() / 60;
        String key = clientIp(request) + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || current.minute != minute ? new Window(minute, 1) : new Window(minute, current.count + 1));
        if (window.count > limit) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.\",\"code\":429}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",", 2)[0].trim();
    }

    private record Window(long minute, int count) {}
}
