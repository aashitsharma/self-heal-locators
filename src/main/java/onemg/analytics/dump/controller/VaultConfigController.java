package onemg.analytics.dump.controller;

import onemg.analytics.dump.JsonConfig;
import onemg.analytics.dump.model.ErrorModel;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/v1/config")
public class VaultConfigController {
    private static final Logger LOGGER = Logger.getLogger(VaultConfigController.class);

    private final RestTemplate restTemplate;

    @Autowired
    public VaultConfigController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("vault/{project}/{env}")
    public ResponseEntity<Object> getConfig(@PathVariable("project") String project,@PathVariable("env") String env) {

        String downstreamUrl = JsonConfig.config.getVaultHost()+"/v1/basecamp/data/sla_service/config"; // URL of downstream API
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", JsonConfig.config.getVaultToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        LOGGER.info("Calling downstream API:");
        LOGGER.info("Project: " + project + ", Env: " + env);
        LOGGER.info("URL: "+ downstreamUrl);
        LOGGER.info("Headers: "+ headers);
        ResponseEntity<String> response = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        try
        {
             response = restTemplate.exchange(
                    downstreamUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            if(response.getStatusCode()== HttpStatusCode.valueOf(200)){
                return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());

            }
            else {
                return new ResponseEntity<>(new ErrorModel().errorResp(response.getStatusCode().value(),response.getBody()), responseHeaders, response.getStatusCode());
            }
        }
        catch (HttpClientErrorException e){
            return new ResponseEntity<>(new ErrorModel().errorResp(e.getStatusCode().value(),e.getMessage()), responseHeaders, e.getStatusCode());

        }
        catch (RestClientException e){
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.INTERNAL_SERVER_ERROR.value(),e.getMessage()), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}