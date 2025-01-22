package onemg.analytics.dump;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;



@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigProperties {
    @JsonProperty("spring.data.mongodb.uri")
    private String springDataMongodbUri;
    @JsonProperty("logging.level.root")
    private String loggingLevel;
    @JsonProperty("logging.level.org.springframework")
    private String loggingSpring;
    @JsonProperty("server.port")
    private int serverPort;
}
