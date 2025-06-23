package onemg.analytics.dump.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import onemg.analytics.dump.JsonConfig;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;
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

            if(request.getRequestURI().contains("/admin/mock-data") && !request.getRequestURI().equalsIgnoreCase("/admin/mock-data/jwt") && Boolean.parseBoolean(JsonConfig.config.getMockObject().get("jwt_enabled").toString())){
                 String header = request.getHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ")) {
                    response.sendError(HttpStatus.FORBIDDEN.value(), "Missing or invalid Authorization header");
                    return;
                }

                String token = header.substring(7);
                if (!JWTUtils.validateToken(token, JsonConfig.config.getMockObject().get("qa_mock_service").toString())) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired JWT token");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String path = request.getRequestURI();
            if(!path.equalsIgnoreCase("/actuator/health"))
                LOGGER.info("Status : "+status+" | Duration : "+duration+" ms | API : "+request.getMethod()+" - "+path + " | Query Params : "+request.getQueryString()+" | Headers : "+CommonUtility.extractHeaders(request.getHeaderNames(),request));
            // Clear MDC after the request completes
            MDC.remove(REFERENCE_KEY);
        }
    }
}

