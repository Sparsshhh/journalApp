package net.engineeringdigest.journalApp.filter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.utils.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Reads "Authorization: Bearer <token>" and, if the token is valid, marks the request as authenticated.
// The authenticated principal is the User entity, so controllers can take it with @AuthenticationPrincipal.
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public JwtFilter(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtUtil.TokenClaims claims = jwtUtil.parse(header.substring(BEARER_PREFIX.length()));
                // Loaded on every request, so deleted users, role changes and logouts take effect immediately.
                User user = claims.userName() == null ? null : userRepository.findByUserName(claims.userName());
                if (user != null && user.getTokenVersion() == claims.tokenVersion()) {
                    List<SimpleGrantedAuthority> authorities = user.getRoles() == null ? List.of()
                            : user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null, authorities));
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Bad or expired token: leave the request unauthenticated; protected routes answer 401.
            }
        }
        chain.doFilter(request, response);
    }
}
