package onemg.analytics.dump.controller.mockcontroller;

import com.fasterxml.jackson.databind.util.JSONPObject;
import jakarta.servlet.http.HttpServletRequest;
import onemg.analytics.dump.JsonConfig;
import onemg.analytics.dump.model.ErrorModel;
import onemg.analytics.dump.utils.CommonUtility;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/v1/mockdummy")
public class MockControllerDummy {


    private final RestTemplate restTemplate = new RestTemplate();
    private final String downstreamBaseUrl = JsonConfig.config.getVaultHost();

    @GetMapping("/mocked-endpoint")
    public ResponseEntity<?> getMockedResponse(HttpServletRequest request, HttpEntity<String> entity, @RequestParam(required = false) String mock) {
        // 1. Match condition: Only serve mock if query param mock=true
        if ("true".equalsIgnoreCase(mock)) {
            return ResponseEntity.ok(CommonUtility.readJsonAsMap("src/main/resources/MockData/Dummy/SKU_232763.json"));
        }
        // 2. Else: Proxy the request
        return proxyUnmatchedRequests(request,entity);
    }

    // Catch-all only within /v1/**
    @RequestMapping("/**")
    public ResponseEntity<?> redirection(HttpServletRequest request, HttpEntity<String> entity){
        return proxyUnmatchedRequests(request,entity);
    }

    private ResponseEntity<?> proxyUnmatchedRequests(HttpServletRequest request, HttpEntity<String> entity) {
        try {
            String subPath = request.getRequestURI().substring(request.getContextPath().length() + "/v1/mockdummy".length());
            String fullDownstreamUrl = downstreamBaseUrl + subPath +
                    (request.getQueryString() != null ? "?" + request.getQueryString() : "");

            HttpHeaders headers = new HttpHeaders();
            Collections.list(request.getHeaderNames()).forEach(header ->
                    headers.set(header, request.getHeader(header)));

            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpEntity<String> proxyRequest = new HttpEntity<>(entity.getBody(), headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    fullDownstreamUrl,
                    method,
                    proxyRequest,
                    byte[].class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());

        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.INTERNAL_SERVER_ERROR.value(),e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
