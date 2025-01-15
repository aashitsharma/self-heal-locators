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
        return configProperties;
    }
}
