package onemg.analytics.dump.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REFERENCE_KEY = "reference";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException,ServletException {
        try {
            String uniqueReference = UUID.randomUUID().toString();
            MDC.put("reference", uniqueReference);
            filterChain.doFilter(request, response);
        } finally {
            // Clear MDC after the request completes
            MDC.remove(REFERENCE_KEY);
        }
    }
}

