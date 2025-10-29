package self.heal.locators;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class AnalyticsDataDumpApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(AnalyticsDataDumpApplication.class);
        JsonConfig js = new JsonConfig();
        js.configProperties();
        app.setDefaultProperties(JsonConfig.props);
        app.run(args);
    }

}
