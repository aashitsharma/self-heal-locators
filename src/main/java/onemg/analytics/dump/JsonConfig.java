package onemg.analytics.dump;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @Bean
    public ConfigProperties configProperties() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        LOGGER.info("Attempting to load config.json...");
        // Path to the root-level config.json file
        File file = new File("config.json");
        ConfigProperties config = objectMapper.readValue(file, ConfigProperties.class);
        LOGGER.info("Loaded ConfigProperties: "+ config);
        setProperties(config);
        return config;
    }

    public static void setProperties(ConfigProperties config){
        System.setProperty("spring.data.mongodb.uri",config.getSpringDataMongodbUri());
        /*System.setProperty("logging.level.root",config.getLoggingLevel());
        System.setProperty("logging.level.org.springframework",config.getLoggingSpring());
        System.setProperty("server.port",String.valueOf(config.getServerPort()));*/

        System.setProperty("logging.level.root","DEBUG");
        System.setProperty("logging.level.org.springframework","DEBUG");
        System.setProperty("server.port","8980");

        props.put("spring.data.mongodb.uri",config.getSpringDataMongodbUri());
        /*props.put("logging.level.root",config.getLoggingLevel());
        props.put("logging.level.org.springframework",config.getLoggingSpring());
        props.put("server.port",String.valueOf(config.getServerPort()));*/

        props.put("logging.level.root","DEBUG");
        props.put("logging.level.org.springframework","DEBUG");
        props.put("server.port","8980");
    }
}
