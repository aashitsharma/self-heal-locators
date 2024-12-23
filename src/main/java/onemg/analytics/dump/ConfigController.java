package onemg.analytics.dump;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Properties;

@RestController
public class ConfigController {

    @Autowired
    private ConfigProperties configProperties;

    @GetMapping("/config")
    public ConfigProperties getConfig() {
        Properties pr = new Properties();
        try{
            pr.load(ConfigController.class.getClassLoader().getResourceAsStream("src/main/resources/application.properties"));
            pr.setProperty("spring.data.mongodb.uri",configProperties.getSPRING_DATA_MONGODB_URI());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return configProperties;
    }
}
