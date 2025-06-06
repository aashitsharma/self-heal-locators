package onemg.analytics.dump;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigProperties {
    @JsonProperty("spring.data.mongodb.uri")
    private String springDataMongodbUri;
    @JsonProperty("logging.level.root")
    private String loggingLevel;
    @JsonProperty("logging.level.org.springframework")
    private String loggingSpring;
    @JsonProperty("server.port")
    private int serverPort;
    @JsonProperty("vault.host")
    private String vaultHost;
    @JsonProperty("vault.token")
    private String vaultToken;
    @JsonProperty("mock.data.object")
    private Map<String,Object> mockObject;

}
