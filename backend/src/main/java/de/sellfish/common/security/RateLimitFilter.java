package de.sellfish.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple per-IP rate limiter using a fixed-window counter.
 * Auth endpoints (/api/auth/**): 10 requests/minute.
 * Everything else: 100 requests/minute.
 * Returns 429 Too Many Requests when exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_LIMIT = 10;
    private static final int API_LIMIT = 100;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp(request);
        String path = request.getRequestURI();
        int limit = path.startsWith("/api/auth/") ? AUTH_LIMIT : API_LIMIT;

        String key = ip + ":" + (path.startsWith("/api/auth/") ? "auth" : "api");
        long now = System.currentTimeMillis();
        long[] bucket = buckets.compute(key, (k, v) -> {
            if (v == null || now - v[0] >= WINDOW_MS) return new long[] {now, 1};
            v[1]++;
            return v;
        });

        if (bucket[1] > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"Rate limit exceeded. Try again in a minute.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
