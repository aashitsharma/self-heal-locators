package onemg.analytics.dump.controller.mockcontroller;

import com.fasterxml.jackson.databind.util.JSONPObject;
import jakarta.servlet.http.HttpServletRequest;
import onemg.analytics.dump.JsonConfig;
import onemg.analytics.dump.enums.VerticalService;
import onemg.analytics.dump.model.ErrorModel;
import onemg.analytics.dump.model.MockDataModel;
import onemg.analytics.dump.repository.MockDataRepository;
import onemg.analytics.dump.utils.CommonUtility;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/mock/{vertical}")
public class MockControllerDummy {

    private static final Logger LOGGER = Logger.getLogger(MockControllerDummy.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String downstreamBaseUrl = JsonConfig.config.getVaultHost();
    //private final String vertical = "pharmacy";
    @Autowired
    private MockDataRepository mockDataRepository;

    @GetMapping("/mocked-endpoint")
    public ResponseEntity<?> getMockedResponse(@PathVariable String vertical,HttpServletRequest request, HttpEntity<String> entity, @RequestParam(required = false) String mock) {
        try {
            // 1. Match condition: Only serve mock if query param mock=true
            if ("true".equalsIgnoreCase(mock)) {
                return ResponseEntity.ok(CommonUtility.readJsonAsMap("MockData/Dummy/SKU_232763.json"));
            }
        }
        catch (Exception e){
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.INTERNAL_SERVER_ERROR.value(),e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // 2. Else: Proxy the request
        return proxyUnmatchedRequests(vertical,request,entity);
    }

    // Catch-all only within /v1/**
    @RequestMapping("/**")
    public ResponseEntity<?> redirection(@PathVariable String vertical,HttpServletRequest request, HttpEntity<String> entity){
        if(VerticalService.isValidVertical(vertical)){
            String subPath = request.getRequestURI().substring(request.getContextPath().length() + returnVerticalPath(vertical).length());
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            LOGGER.info("Sub Path : "+subPath+" | Method : "+method.name()+" | Vertical : "+vertical);
            Optional<MockDataModel> mockedData = mockDataRepository.findByUriAndVerticalAndMethod(subPath, vertical, method.name());
            if (mockedData.isPresent()) {
                Map<String, Object> mockedResponse = mockedData.get().getResponse();
                return ResponseEntity.status(mockedData.get().getResponseCode()).contentType(MediaType.APPLICATION_JSON).body(mockedResponse);
            }
            return proxyUnmatchedRequests(vertical,request,entity);
        }
        else{
             return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.BAD_REQUEST.value(),"Requested vertical doesn't exist in records : "+vertical), HttpStatus.BAD_REQUEST);
        }
    }

    private ResponseEntity<?> proxyUnmatchedRequests(@PathVariable String vertical,HttpServletRequest request, HttpEntity<String> entity) {
        try {
            String subPath = request.getRequestURI().substring(request.getContextPath().length() + returnVerticalPath(vertical).length());
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

    private String returnVerticalPath(String vertical){
        return "/mock/"+vertical;
    }
}
