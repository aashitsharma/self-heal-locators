package onemg.analytics.dump;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

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

       /* Properties pr = new Properties();
        try{
            File reader = new File("src/main/resources/application.properties");
            if (reader.exists()) {
                pr.load(new FileReader(reader));
                LOGGER.info("FILE LOADED FROM SOURCE ");
            }
            if (!reader.exists()) {
                pr.load(ConfigController.class.getResourceAsStream("/application.properties"));
                LOGGER.info("FILE LOADED FROM ROOT ");
            }
            LOGGER.info("Mongo URI is : "+config.getSpringDataMongodbUri());
            pr.setProperty("spring.data.mongodb.uri",config.getSpringDataMongodbUri());
            LOGGER.info("Fetching Property Details : "+pr.getProperty("spring.data.mongodb.uri"));
        }
        catch (Exception e){
            e.printStackTrace();
        }*/

        return config;
    }
}
