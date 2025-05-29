package onemg.analytics.dump.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(RequestLoggingFilter.class);
    private static final String REFERENCE_KEY = "reference";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException,ServletException {
        long startTime = System.currentTimeMillis();
        try {
            String uniqueReference = UUID.randomUUID().toString();
            MDC.put("reference", "REF-ID-"+uniqueReference);
            response.addHeader("request_id",uniqueReference);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String path = request.getRequestURI();
            LOGGER.info("Status : "+status+" | Duration : "+duration+" ms | API : "+path + " | Query Params : "+request.getQueryString());
            // Clear MDC after the request completes
            MDC.remove(REFERENCE_KEY);
        }
    }
}

