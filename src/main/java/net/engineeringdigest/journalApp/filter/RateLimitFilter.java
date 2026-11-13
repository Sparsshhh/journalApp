package net.engineeringdigest.journalApp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits how often one client address may call the login, sign-up and token-refresh endpoints,
 * to slow down password guessing. Counts are kept in memory, so they are per app instance and
 * reset on restart. Behind a proxy, set server.forward-headers-strategy so the real client
 * address is used instead of the proxy's.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/public/login", "/public/createUser", "/public/refresh");
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(long startMs, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public RateLimitFilter(@Value("${ratelimit.auth.max-requests:10}") int maxRequests,
                           @Value("${ratelimit.auth.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!tryAcquire(request.getRemoteAddr(), System.currentTimeMillis())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(windowMs / 1000));
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Too many attempts. Try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    // Visible for tests. Returns false once the client has used up its requests for the current window.
    boolean tryAcquire(String client, long nowMs) {
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(window -> nowMs - window.startMs() >= windowMs);
        }
        Window window = windows.compute(client, (key, current) ->
                current == null || nowMs - current.startMs() >= windowMs
                        ? new Window(nowMs, 1)
                        : new Window(current.startMs(), current.count() + 1));
        return window.count() <= maxRequests;
    }
}
