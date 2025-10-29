package self.heal.locators;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class JsonConfig {
    private static final Logger LOGGER = Logger.getLogger(JsonConfig.class);
    public static Map<String, Object> props = new HashMap<>();
    public static ConfigProperties config =null;
    @Bean
    public ConfigProperties configProperties() {
        ObjectMapper objectMapper = new ObjectMapper();
        LOGGER.info("Attempting to load config.json...");
        // Path to the root-level config.json file
        File file = new File("config.json");
        if (!file.exists()) {
            file =new File("/config.json");
        }
        try{
            config=objectMapper.readValue(file, ConfigProperties.class);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        LOGGER.info("Loaded ConfigProperties: "+ config);
        setProperties(config);
        return config;
    }

    public static void setProperties(ConfigProperties config){
        System.setProperty("spring.data.mongodb.uri",config.getSpringDataMongodbUri());
        /*System.setProperty("logging.level.root",config.getLoggingLevel());
        System.setProperty("logging.level.org.springframework",config.getLoggingSpring());
        System.setProperty("server.port",String.valueOf(config.getServerPort()));*/

        System.setProperty("logging.level.root","INFO");
        System.setProperty("logging.level.org.springframework","INFO");
        System.setProperty("server.port","8980");
        System.setProperty("management.endpoints.web.exposure.include","*");
        System.setProperty("management.endpoint.health.show-details","always");
        System.setProperty("spring.servlet.multipart.max-file-size", "10MB");
        System.setProperty("spring.servlet.multipart.max-request-size", "10MB");

        props.put("spring.data.mongodb.uri",config.getSpringDataMongodbUri());
        /*props.put("logging.level.root",config.getLoggingLevel());
        props.put("logging.level.org.springframework",config.getLoggingSpring());
        props.put("server.port",String.valueOf(config.getServerPort()));*/

        props.put("logging.level.root","INFO");
        props.put("logging.level.org.springframework","INFO");
        props.put("server.port","8980");
        props.put("management.endpoints.web.exposure.include","*");
        props.put("management.endpoint.health.show-details","always");
        props.put("spring.servlet.multipart.max-file-size", "10MB");
        props.put("spring.servlet.multipart.max-request-size", "10MB");
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
