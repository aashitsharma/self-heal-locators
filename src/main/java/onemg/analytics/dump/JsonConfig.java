package onemg.analytics.dump;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class JsonConfig {

    @Bean
    public ConfigProperties configProperties() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Path to the root-level config.json file
        File file = new File("config.json");
        if (!file.exists()) {
            throw new IOException("config.json not found at root level: " + file.getAbsolutePath());
        }

        return objectMapper.readValue(file, ConfigProperties.class);
    }
}
