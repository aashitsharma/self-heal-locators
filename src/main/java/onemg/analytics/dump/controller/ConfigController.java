package onemg.analytics.dump.controller;

import onemg.analytics.dump.ConfigProperties;
import onemg.analytics.dump.JsonConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/v1/config")
public class ConfigController {

    private final RestTemplate restTemplate;
    private JsonConfig properties ;
    @Autowired
    public ConfigController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("vault/{project}/{env}")
    public ResponseEntity<Object> getConfig(@PathVariable("project") String project,@PathVariable("env") String env) {

        properties.configProperties().getVaultToken();
        String downstreamUrl = properties.configProperties().getVaultHost()+"/v1/basecamp/data/sla_service/config'"; // URL of downstream API
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", properties.configProperties().getVaultToken());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                downstreamUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

}