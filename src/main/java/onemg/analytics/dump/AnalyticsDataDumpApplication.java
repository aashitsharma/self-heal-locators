package onemg.analytics.dump;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AnalyticsDataDumpApplication {

    public static void main(String[] args) {
        ConfigController obj = new ConfigController();
        obj.getConfig();
        SpringApplication.run(AnalyticsDataDumpApplication.class, args);
    }
}
