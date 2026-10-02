package dsa_hub_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String[] segments = request.getServletPath().split("/");
        if ("/api/health".equals(request.getServletPath())
                || "/api/sheets".equals(request.getServletPath())
                || "/api/problems".equals(request.getServletPath())) {
            return true;
        }

        if (segments.length == 4 && "api".equals(segments[1])) {
            return "sheets".equals(segments[2]) || "problems".equals(segments[2]);
        }

        return segments.length == 5
                && "api".equals(segments[1])
                && (("sheets".equals(segments[2])
                        && ("problems".equals(segments[4]) || "problem-occurrences".equals(segments[4])))
                    || ("problems".equals(segments[2])
                        && ("sheets".equals(segments[4])
                            || "links".equals(segments[4])
                            || "topics".equals(segments[4]))));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Long userId = Long.valueOf(jwt.getSubject());
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
