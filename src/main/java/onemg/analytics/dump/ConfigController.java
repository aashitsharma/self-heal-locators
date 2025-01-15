package onemg.analytics.dump;

import onemg.analytics.dump.controller.AnalyticsDataController;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

@RestController
public class ConfigController {
    private static final Logger LOGGER = Logger.getLogger(ConfigController.class);

    @Autowired
    private ConfigProperties configProperties = new ConfigProperties();

    @GetMapping("/config")
    public ConfigProperties getConfig() {
        Properties pr = new Properties();
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
            LOGGER.info("Mongo URI is : "+configProperties.getSpringDataMongodbUri());
            pr.setProperty("spring.data.mongodb.uri",configProperties.getSpringDataMongodbUri());
            LOGGER.info("Fetching Property Details : "+pr.getProperty("spring.data.mongodb.uri"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return configProperties;
    }
}
