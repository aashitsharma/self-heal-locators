package onemg.analytics.dump.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration properties for RAG (Retrieval-Augmented Generation) system
 */
@ConfigurationProperties(prefix = "rag")
@Data
public class RagConfiguration {
    
    /**
     * Maximum context length in characters that will be sent to LLM
     */
    private int maxContextLength = 4000;
    
    /**
     * Size of each chunk in characters when splitting page source (DEPRECATED)
     * Kept for backward compatibility - now using 5-chunk approach
     */
    private int chunkSize = 500;
    
    /**
     * Overlap size between chunks in characters (DEPRECATED)
     * Kept for backward compatibility - now using 5-chunk approach
     */
    private int overlapSize = 50;
    
    /**
     * Minimum relevance threshold for including chunks (DEPRECATED)
     * Kept for backward compatibility - now using simple 5-chunk approach
     */
    private double relevanceThreshold = 0.1;
    
    /**
     * Whether RAG is enabled or not
     */
    private boolean enabled = true;
    
    /**
     * Weight for ID attribute matches in relevance scoring
     */
    private double idMatchWeight = 3.0;
    
    /**
     * Weight for resource-id attribute matches in relevance scoring
     */
    private double resourceIdMatchWeight = 2.5;
    
    /**
     * Weight for class attribute matches in relevance scoring
     */
    private double classMatchWeight = 1.5;
    
    /**
     * Weight for text content matches in relevance scoring
     */
    private double textMatchWeight = 2.0;
    
    /**
     * Weight for tag name matches in relevance scoring
     */
    private double tagMatchWeight = 1.0;
    
    /**
     * Weight for other attribute matches in relevance scoring
     */
    private double otherAttributeMatchWeight = 1.0;
    
    /**
     * Maximum number of chunks to analyze for performance
     */
    private int maxChunksToAnalyze = 100;
    
    /**
     * Whether to enable performance tracking
     */
    private boolean performanceTrackingEnabled = true;
    
    /**
     * Whether to enable detailed logging
     */
    private boolean detailedLoggingEnabled = true;
    
    /**
     * Whether to enable debug mode with extensive logging
     */
    private boolean debugMode = false;
}