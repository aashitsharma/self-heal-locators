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
        return objectMapper.readValue(new File("config.json"), ConfigProperties.class);
    }
}
