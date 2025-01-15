package onemg.analytics.dump;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AnalyticsDataDumpApplication {

    public static void main(String[] args) {
        ConfigController obj = new ConfigController();
        obj.getConfig();
        SpringApplication.run(AnalyticsDataDumpApplication.class, args);
    }

}
