package onemg.analytics.dump.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable RAG configuration properties
 */
@Configuration
@EnableConfigurationProperties(RagConfiguration.class)
public class RagConfigurationSetup {
    // This class enables the RagConfiguration properties
}