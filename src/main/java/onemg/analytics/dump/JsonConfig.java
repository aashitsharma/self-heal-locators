package onemg.analytics.dump;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class JsonConfig {
    private static final Logger LOGGER = Logger.getLogger(JsonConfig.class);

    @Bean
    public ConfigProperties configProperties() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        LOGGER.info("Attempting to load config.json...");
        // Path to the root-level config.json file
        File file = new File("config.json");
        if (!file.exists()) {
            throw new IOException("config.json not found at root level: " + file.getAbsolutePath());
        }

        ConfigProperties config = objectMapper.readValue(file, ConfigProperties.class);
        LOGGER.info("Loaded ConfigProperties: "+ config);
        return config;
    }
}
