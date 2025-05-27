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

        String downstreamUrl = JsonConfig.config.getVaultHost()+"/v1/basecamp/data/sla_service/config'"; // URL of downstream API
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", JsonConfig.config.getVaultToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        LOGGER.info("Calling downstream API:");
        LOGGER.info("URL: "+ downstreamUrl);
        LOGGER.info("Headers: "+ headers);

        ResponseEntity<String> response = restTemplate.exchange(
                downstreamUrl,
                HttpMethod.GET,
                entity,
                String.class
        );
        if(response.getStatusCode()== HttpStatusCode.valueOf(200)){
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }
        else {
            return ResponseEntity.status(response.getStatusCode()).body(new ErrorModel().errorResp(response.getStatusCode().value(),response.getBody()));
        }

    }

}